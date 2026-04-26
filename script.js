const FEEDS = {
  latest: 'https://www.eenadu.net/rss/latest-news.xml',
  andhra: 'https://www.eenadu.net/rss/andhra-pradesh.xml',
  telangana: 'https://www.eenadu.net/rss/telangana.xml'
};

const RSS2JSON = 'https://api.rss2json.com/v1/api.json?rss_url=';
const GEMINI_API_KEY = 'YOUR_GEMINI_API_KEY'; // Move to Netlify env var in production.
const GEMINI_MODEL = 'gemini-1.5-flash';
const CACHE_TTL_MS = 10 * 60 * 1000;

const grid = document.getElementById('news-grid');
const statusEl = document.getElementById('status');
const feedSelect = document.getElementById('feed-select');
const languageSelect = document.getElementById('language-select');

feedSelect.addEventListener('change', bootstrap);
languageSelect.addEventListener('change', bootstrap);

document.addEventListener('DOMContentLoaded', bootstrap);

async function bootstrap() {
  const feedKey = feedSelect.value;
  const lang = languageSelect.value;
  const cacheKey = `news:${feedKey}:${lang}`;

  statusEl.textContent = 'Loading live feed...';
  const cached = readCache(cacheKey);
  if (cached) {
    statusEl.textContent = 'Loaded from cache (valid for 10 minutes).';
    render(cached);
    updateJsonLd(cached, feedKey, lang);
    return;
  }

  try {
    const raw = await fetchFeed(FEEDS[feedKey]);
    const prepared = raw.slice(0, 16).map((item) => ({
      title: stripHtml(item.title || ''),
      summary: stripHtml(item.description || ''),
      link: item.link,
      image: item.enclosure?.link || item.thumbnail || '',
      pubDate: item.pubDate
    }));

    const rewritten = await rewriteBatch(prepared, lang);
    writeCache(cacheKey, rewritten);
    render(rewritten);
    updateJsonLd(rewritten, feedKey, lang);
    statusEl.textContent = 'Live feed fetched and AI-processed.';
  } catch (error) {
    console.error(error);
    statusEl.textContent = 'Unable to fetch/process now. Showing fallback if available.';
    const fallback = readAnyFeedFallback(feedKey);
    if (fallback) {
      render(fallback);
      updateJsonLd(fallback, feedKey, lang);
    } else {
      grid.innerHTML = '<p class="text-red-300">No data available.</p>';
    }
  }
}

async function fetchFeed(feedUrl) {
  const response = await fetch(`${RSS2JSON}${encodeURIComponent(feedUrl)}`);
  if (!response.ok) throw new Error('RSS fetch failed');
  const data = await response.json();
  if (!data.items) throw new Error('Invalid RSS payload');
  return data.items;
}

async function rewriteBatch(items, language) {
  const targetLanguage = language === 'te' ? 'Telugu' : language === 'hi' ? 'Hindi' : 'English';
  const prompt = `You are a professional journalist and SEO editor.
Input: a JSON array of Telugu news entries with title and summary.
Task:
1) Translate each entry into ${targetLanguage} (if Telugu requested, keep Telugu).
2) Rewrite title and summary to be original, SEO-friendly, factual, concise, and non-plagiarized.
3) Keep a neutral journalistic tone.
Return ONLY valid JSON array with keys: title, summary.`;

  // For production, route this call through a Netlify Function as middleware.
  // Example endpoint: /.netlify/functions/gemini-rewrite
  const body = {
    contents: [{ parts: [{ text: `${prompt}\n\n${JSON.stringify(items)}` }] }],
    generationConfig: { temperature: 0.4 }
  };

  const response = await fetch(`https://generativelanguage.googleapis.com/v1beta/models/${GEMINI_MODEL}:generateContent?key=${GEMINI_API_KEY}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json; charset=UTF-8' },
    body: JSON.stringify(body)
  });

  if (!response.ok) {
    throw new Error('Gemini rewrite failed');
  }

  const result = await response.json();
  const text = result.candidates?.[0]?.content?.parts?.[0]?.text || '[]';
  const parsed = safeJsonParse(text, []);

  return items.map((item, idx) => ({
    ...item,
    title: parsed[idx]?.title || item.title,
    summary: parsed[idx]?.summary || item.summary
  }));
}

function render(items) {
  grid.innerHTML = items
    .map((article, idx) => {
      const featureClass = idx % 7 === 0
        ? 'md:col-span-2 md:row-span-2'
        : idx % 5 === 0
          ? 'xl:col-span-2'
          : 'col-span-1';

      return `
        <article class="news-card ${featureClass}">
          ${article.image ? `<img src="${article.image}" alt="${escapeHtml(article.title)}" class="w-full h-24 object-cover rounded-xl" loading="lazy"/>` : ''}
          <h2>${escapeHtml(article.title)}</h2>
          <p>${escapeHtml(article.summary)}</p>
          <a href="${article.link}" target="_blank" rel="noopener noreferrer">Read full story →</a>
        </article>
      `;
    })
    .join('');
}

function updateJsonLd(items, feedKey, lang) {
  const graph = items.map((item) => ({
    '@type': 'NewsArticle',
    headline: item.title,
    description: item.summary,
    inLanguage: lang,
    datePublished: item.pubDate,
    url: item.link,
    image: item.image || undefined,
    publisher: { '@type': 'Organization', name: 'Eenadu Smart News' }
  }));

  document.getElementById('jsonld-container').textContent = JSON.stringify({
    '@context': 'https://schema.org',
    '@graph': graph,
    mainEntityOfPage: `/${feedKey}`
  });
}

function readCache(key) {
  const raw = localStorage.getItem(key);
  if (!raw) return null;
  const data = safeJsonParse(raw, null);
  if (!data?.timestamp || Date.now() - data.timestamp > CACHE_TTL_MS) return null;
  return data.items;
}

function writeCache(key, items) {
  localStorage.setItem(key, JSON.stringify({ timestamp: Date.now(), items }));
}

function readAnyFeedFallback(feedKey) {
  for (const lang of ['te', 'en', 'hi']) {
    const items = readCache(`news:${feedKey}:${lang}`);
    if (items) return items;
  }
  return null;
}

function stripHtml(value) {
  return value.replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim();
}

function escapeHtml(value) {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}

function safeJsonParse(value, fallback) {
  try {
    const start = value.indexOf('[');
    const end = value.lastIndexOf(']');
    if (start >= 0 && end >= 0) {
      return JSON.parse(value.slice(start, end + 1));
    }
    return JSON.parse(value);
  } catch {
    return fallback;
  }
}
