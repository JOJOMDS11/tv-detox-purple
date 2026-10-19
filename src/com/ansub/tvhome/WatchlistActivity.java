package com.ansub.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

public class WatchlistActivity extends Activity {

    private static final String JSON_URL =
            "https://raw.githubusercontent.com/JOJOMDS11/tv-detox-purple/watchlist-data/public/watchlist.json";
    private static final String CACHE_FILE = "watchlist.json";
    private static final long MAX_AGE_MS = 30L * 60L * 1000L;
    private static final String ALL = "Todos";
    private static final int COLS = 5;
    private static final int ROWS = 3;
    private static final int PER_PAGE = COLS * ROWS;

    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_DIM = 0xA6FFFFFF;
    private static final int GOLD = 0xFFF5C518;
    private static final int CARD = 0x14FFFFFF;
    private static final int CARD_ON = 0x3DFFFFFF;

    private static final Map<String, String> GENRES_PT = new HashMap<>();
    static {
        GENRES_PT.put("Action", "Ação");
        GENRES_PT.put("Adventure", "Aventura");
        GENRES_PT.put("Animation", "Animação");
        GENRES_PT.put("Biography", "Biografia");
        GENRES_PT.put("Comedy", "Comédia");
        GENRES_PT.put("Crime", "Crime");
        GENRES_PT.put("Documentary", "Documentário");
        GENRES_PT.put("Drama", "Drama");
        GENRES_PT.put("Family", "Família");
        GENRES_PT.put("Fantasy", "Fantasia");
        GENRES_PT.put("Film-Noir", "Film noir");
        GENRES_PT.put("Game-Show", "Game show");
        GENRES_PT.put("History", "História");
        GENRES_PT.put("Horror", "Terror");
        GENRES_PT.put("Music", "Música");
        GENRES_PT.put("Musical", "Musical");
        GENRES_PT.put("Mystery", "Mistério");
        GENRES_PT.put("News", "Notícias");
        GENRES_PT.put("Reality-TV", "Reality");
        GENRES_PT.put("Romance", "Romance");
        GENRES_PT.put("Sci-Fi", "Ficção científica");
        GENRES_PT.put("Short", "Curta");
        GENRES_PT.put("Sport", "Esporte");
        GENRES_PT.put("Talk-Show", "Talk show");
        GENRES_PT.put("Thriller", "Suspense");
        GENRES_PT.put("War", "Guerra");
        GENRES_PT.put("Western", "Faroeste");
    }

    private static final class Item {
        String id;
        String title;
        String poster = "";
        int year;
        double rating;
        int meta;
        boolean series;
        int kind; // 1 filme, 2 filme pra TV, 3 série, 0 outros
        List<String> genres = new ArrayList<>();
    }

    private static final int T_GENRE = 0, T_YEAR = 1, T_KIND = 2, T_SORT = 3;

    private static final class Chip {
        int type;
        String genre = "";
        int value = -1; // década, tipo ou ordem
        TextView view;
        GradientDrawable bg;
    }

    private final List<Item> all = new ArrayList<>();
    private final List<Item> shown = new ArrayList<>();
    private final List<String> genreNames = new ArrayList<>();
    private final Map<String, Integer> genreCounts = new HashMap<>();
    private final List<Chip> chips = new ArrayList<>();
    private String current = ALL;
    private int currentDecade = -1;
    private int currentKind = 0;
    private int currentSort = 0;
    private final int[] kindCounts = new int[4];
    private LinearLayout kindRow;
    private final Map<Integer, Integer> decadeCounts = new HashMap<>();
    private final List<Integer> decadeList = new ArrayList<>();
    private LinearLayout yearRow;
    private String lastRaw = "";
    private int page = 0;
    private volatile int generation = 0;
    private int posterW;

    private final FrameLayout[] cards = new FrameLayout[PER_PAGE];
    private final ImageView[] posters = new ImageView[PER_PAGE];
    private final TextView[] titles = new TextView[PER_PAGE];
    private final TextView[] ratings = new TextView[PER_PAGE];
    private final TextView[] metaViews = new TextView[PER_PAGE];
    private final GradientDrawable[] metaBgs = new GradientDrawable[PER_PAGE];
    private final TextView[] metas = new TextView[PER_PAGE];
    private final TextView[] genreViews = new TextView[PER_PAGE];

    private LinearLayout chipRow;
    private TextView headerTitle;
    private TextView headerCount;
    private TextView pageView;
    private TextView emptyView;

    private LruCache<String, Bitmap> memory;
    private ExecutorService pool;
    private File posterDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {0xFF1B0C30, 0xFF0E0719, 0xFF07030F});
        getWindow().setBackgroundDrawable(bg);
        memory = new LruCache<String, Bitmap>(10 * 1024 * 1024) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };
        pool = Executors.newFixedThreadPool(3);
        posterDir = new File(getCacheDir(), "posters");
        posterDir.mkdirs();
        setContentView(buildLayout());
        loadCache();
        refreshIfStale();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pool.shutdownNow();
        memory.evictAll();
    }

    private View buildLayout() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int wdp = Math.round(dm.widthPixels / dm.density);
        int hdp = Math.round(dm.heightPixels / dm.density);
        int padX = 40;
        int gap = 12;
        int cellW = (wdp - 2 * padX - gap * (COLS - 1)) / COLS;
        int cellH = (hdp - 238) / ROWS;
        int pw = Math.min((cellH - 16) * 2 / 3, cellW * 52 / 100);
        posterW = dp(pw);

        FrameLayout root = new FrameLayout(this);

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(dp(padX), dp(28), dp(padX), dp(20));
        root.addView(main, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        headerTitle = text(26, TEXT, Typeface.BOLD);
        headerCount = text(18, TEXT_DIM, Typeface.NORMAL);
        headerCount.setPadding(dp(14), dp(4), 0, 0);
        pageView = text(16, TEXT_DIM, Typeface.NORMAL);
        View spacer = new View(this);
        header.addView(headerTitle);
        header.addView(headerCount);
        header.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1f));
        header.addView(pageView);
        main.addView(header, new LinearLayout.LayoutParams(-1, dp(40)));

        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setClipToPadding(false);
        chipRow = new LinearLayout(this);
        chipRow.setOrientation(LinearLayout.HORIZONTAL);
        chipRow.setPadding(dp(4), dp(4), dp(4), dp(4));
        scroll.addView(chipRow, new FrameLayout.LayoutParams(-2, -1));
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(-1, dp(40));
        scrollLp.topMargin = dp(4);
        scrollLp.bottomMargin = dp(2);
        main.addView(scroll, scrollLp);

        HorizontalScrollView yscroll = new HorizontalScrollView(this);
        yscroll.setHorizontalScrollBarEnabled(false);
        yscroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        yscroll.setClipToPadding(false);
        yearRow = new LinearLayout(this);
        yearRow.setOrientation(LinearLayout.HORIZONTAL);
        yearRow.setPadding(dp(4), dp(2), dp(4), dp(2));
        yscroll.addView(yearRow, new FrameLayout.LayoutParams(-2, -1));
        LinearLayout.LayoutParams yearLp = new LinearLayout.LayoutParams(-1, dp(34));
        yearLp.bottomMargin = dp(2);
        main.addView(yscroll, yearLp);

        HorizontalScrollView kscroll = new HorizontalScrollView(this);
        kscroll.setHorizontalScrollBarEnabled(false);
        kscroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        kscroll.setClipToPadding(false);
        kindRow = new LinearLayout(this);
        kindRow.setOrientation(LinearLayout.HORIZONTAL);
        kindRow.setPadding(dp(4), dp(2), dp(4), dp(2));
        kscroll.addView(kindRow, new FrameLayout.LayoutParams(-2, -1));
        LinearLayout.LayoutParams kindLp = new LinearLayout.LayoutParams(-1, dp(34));
        kindLp.bottomMargin = dp(10);
        main.addView(kscroll, kindLp);

        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        for (int r = 0; r < ROWS; r++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int c = 0; c < COLS; c++) {
                FrameLayout card = buildCard(r * COLS + c, pw);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1f);
                if (c > 0) lp.leftMargin = dp(gap);
                row.addView(card, lp);
            }
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, 0, 1f);
            if (r > 0) rowLp.topMargin = dp(gap);
            grid.addView(row, rowLp);
        }
        main.addView(grid, new LinearLayout.LayoutParams(-1, 0, 1f));

        emptyView = text(20, TEXT_DIM, Typeface.NORMAL);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setText("Carregando…");
        root.addView(emptyView, new FrameLayout.LayoutParams(-1, -1));

        updateHeader();
        return root;
    }

    private FrameLayout buildCard(final int index, int pw) {
        FrameLayout card = new FrameLayout(this);
        final GradientDrawable bg = rounded(CARD, 12);
        card.setBackground(bg);
        card.setFocusable(true);
        card.setClickable(true);
        card.setTag(Integer.valueOf(index));
        card.setOnFocusChangeListener((v, has) -> {
            bg.setColor(has ? CARD_ON : CARD);
            bg.setStroke(has ? dp(2) : 0, 0xFFFFFFFF);
        });
        card.setOnClickListener(v -> {
            int at = page * PER_PAGE + index;
            if (at < shown.size()) openInStremio(shown.get(at));
        });

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.HORIZONTAL);
        inner.setPadding(dp(8), dp(8), dp(8), dp(8));

        ImageView poster = new ImageView(this);
        poster.setScaleType(ImageView.ScaleType.CENTER_CROP);
        poster.setBackground(rounded(0x22FFFFFF, 8));
        poster.setClipToOutline(true);
        FrameLayout posterBox = new FrameLayout(this);
        posterBox.addView(poster, new FrameLayout.LayoutParams(-1, -1));
        // Metascore: selo colorido no canto do pôster (verde 61+, amarelo 40-60, vermelho abaixo).
        TextView metaView = text(11, TEXT, Typeface.BOLD);
        GradientDrawable metaBg = rounded(0xFF2E9E3E, 4);
        metaView.setBackground(metaBg);
        metaView.setGravity(Gravity.CENTER);
        metaView.setPadding(dp(4), dp(1), dp(4), dp(1));
        metaView.setVisibility(View.GONE);
        FrameLayout.LayoutParams badgeLp = new FrameLayout.LayoutParams(-2, -2, Gravity.START | Gravity.TOP);
        badgeLp.setMargins(dp(3), dp(3), 0, 0);
        posterBox.addView(metaView, badgeLp);
        inner.addView(posterBox, new LinearLayout.LayoutParams(dp(pw), -1));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(10), dp(2), dp(2), 0);

        TextView title = text(14, TEXT, Typeface.BOLD);
        title.setMaxLines(3);
        title.setEllipsize(TextUtils.TruncateAt.END);
        TextView rating = text(14, TEXT, Typeface.BOLD);
        TextView meta = text(12, TEXT_DIM, Typeface.NORMAL);
        meta.setSingleLine(true);
        TextView genre = text(12, TEXT_DIM, Typeface.NORMAL);
        genre.setMaxLines(2);
        genre.setEllipsize(TextUtils.TruncateAt.END);

        col.addView(title);
        LinearLayout.LayoutParams ratingLp = new LinearLayout.LayoutParams(-2, -2);
        ratingLp.topMargin = dp(8);
        col.addView(rating, ratingLp);
        LinearLayout.LayoutParams metaLp = new LinearLayout.LayoutParams(-2, -2);
        metaLp.topMargin = dp(6);
        col.addView(meta, metaLp);
        LinearLayout.LayoutParams genreLp = new LinearLayout.LayoutParams(-2, -2);
        genreLp.topMargin = dp(4);
        col.addView(genre, genreLp);
        inner.addView(col, new LinearLayout.LayoutParams(0, -1, 1f));

        card.addView(inner, new FrameLayout.LayoutParams(-1, -1));

        cards[index] = card;
        posters[index] = poster;
        metaViews[index] = metaView;
        metaBgs[index] = metaBg;
        titles[index] = title;
        ratings[index] = rating;
        metas[index] = meta;
        genreViews[index] = genre;
        card.setVisibility(View.INVISIBLE);
        card.setFocusable(false);
        return card;
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(dp(radiusDp));
        d.setColor(color);
        return d;
    }

    private TextView text(int sp, int color, int style) {
        TextView t = new TextView(this);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setTypeface(Typeface.SANS_SERIF, style);
        t.setIncludeFontPadding(false);
        return t;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void rebuildChips() {
        chipRow.removeAllViews();
        yearRow.removeAllViews();
        kindRow.removeAllViews();
        chips.clear();
        for (String g : genreNames) {
            Chip chip = new Chip();
            chip.type = T_GENRE;
            chip.genre = g;
            Integer n = g.equals(ALL) ? Integer.valueOf(all.size()) : genreCounts.get(g);
            addChip(chipRow, chip, n == null ? g : g + "  " + n, 16, 0);
        }
        for (int d : decadeList) {
            Chip chip = new Chip();
            chip.type = T_YEAR;
            chip.value = d;
            Integer n = d == -1 ? Integer.valueOf(all.size()) : decadeCounts.get(d);
            addChip(yearRow, chip, decadeLabel(d) + (n == null ? "" : "  " + n), 14, 0);
        }
        addKindChip(0, "Todos os tipos", all.size());
        addKindChip(1, "Filmes", kindCounts[1]);
        addKindChip(2, "Filmes pra TV", kindCounts[2]);
        addKindChip(3, "Séries", kindCounts[3]);
        addSortChip(0, "Ordem padrão", 24);
        addSortChip(1, "Maior nota IMDb", 0);
        addSortChip(2, "Maior Metascore", 0);
        refreshChips();
    }

    private void addKindChip(int kind, String label, int n) {
        if (kind != 0 && n == 0) return;
        Chip chip = new Chip();
        chip.type = T_KIND;
        chip.value = kind;
        addChip(kindRow, chip, kind == 0 ? label : label + "  " + n, 14, 0);
    }

    private void addSortChip(int sort, String label, int gapDp) {
        Chip chip = new Chip();
        chip.type = T_SORT;
        chip.value = sort;
        addChip(kindRow, chip, label, 14, gapDp);
    }

    private void addChip(LinearLayout row, final Chip chip, String label, int sp, int gapDp) {
        chip.bg = rounded(CARD, 20);
        TextView v = text(sp, TEXT_DIM, Typeface.NORMAL);
        v.setText(label);
        v.setSingleLine(true);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(18), 0, dp(18), 0);
        v.setBackground(chip.bg);
        v.setFocusable(true);
        v.setClickable(true);
        v.setTag(chip);
        v.setOnFocusChangeListener((view, has) -> styleChip(chip, has));
        v.setOnClickListener(view -> {
            switch (chip.type) {
                case T_YEAR: currentDecade = chip.value; break;
                case T_KIND: currentKind = chip.value; break;
                case T_SORT: currentSort = chip.value; break;
                default: current = chip.genre;
            }
            page = 0;
            refreshChips();
            filter();
            render();
            focusCard(0);
        });
        chip.view = v;
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -1);
        lp.leftMargin = dp(gapDp);
        lp.rightMargin = dp(8);
        row.addView(v, lp);
        chips.add(chip);
    }

    private boolean isActive(Chip c) {
        switch (c.type) {
            case T_YEAR: return c.value == currentDecade;
            case T_KIND: return c.value == currentKind;
            case T_SORT: return c.value == currentSort;
            default: return c.genre.equals(current);
        }
    }

    private static int rowOf(Chip c) {
        return c.type == T_GENRE ? 0 : c.type == T_YEAR ? 1 : 2;
    }

    private static String kindLabel(int k) {
        return k == 1 ? "Filmes" : k == 2 ? "Filmes pra TV" : "Séries";
    }

    private static int bucket(int year) {
        return year <= 0 ? -2 : year < 1970 ? 1960 : year / 10 * 10;
    }

    private static String decadeLabel(int d) {
        return d == -1 ? "Todos os anos" : d == 1960 ? "Até 1969" : d + "s";
    }

    private void refreshChips() {
        for (Chip c : chips) styleChip(c, c.view.hasFocus());
    }

    private void styleChip(Chip c, boolean focused) {
        boolean active = isActive(c);
        c.bg.setColor(focused ? CARD_ON : active ? 0x33FFFFFF : CARD);
        c.bg.setStroke(focused ? dp(2) : 0, 0xFFFFFFFF);
        c.view.setTextColor(active || focused ? TEXT : TEXT_DIM);
        c.view.setTypeface(Typeface.SANS_SERIF, active ? Typeface.BOLD : Typeface.NORMAL);
    }

    private void loadCache() {
        File f = new File(getFilesDir(), CACHE_FILE);
        if (!f.exists()) return;
        try {
            String raw = readAll(new FileInputStream(f));
            List<Item> parsed = parse(raw);
            if (!parsed.isEmpty()) {
                lastRaw = raw;
                setItems(parsed);
            }
        } catch (Exception ignored) {}
    }

    private void refreshIfStale() {
        File f = new File(getFilesDir(), CACHE_FILE);
        // Cache antigo sem gênero/nota (JSON de antes da atualização) é refeito na hora, sem esperar os 30 min.
        if (f.exists() && !all.isEmpty() && hasMeta()
                && System.currentTimeMillis() - f.lastModified() < MAX_AGE_MS) return;
        pool.execute(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(JSON_URL).openConnection();
                c.setConnectTimeout(10000);
                c.setReadTimeout(20000);
                if (c.getResponseCode() != 200) throw new Exception("http " + c.getResponseCode());
                final String raw = readAll(c.getInputStream());
                final List<Item> parsed = parse(raw);
                if (parsed.isEmpty()) throw new Exception("vazio");
                FileOutputStream out = new FileOutputStream(new File(getFilesDir(), CACHE_FILE));
                out.write(raw.getBytes("UTF-8"));
                out.close();
                runOnUiThread(() -> {
                    if (isFinishing() || raw.equals(lastRaw)) return;
                    lastRaw = raw;
                    setItems(parsed);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (!isFinishing() && all.isEmpty()) emptyView.setText("Não foi possível carregar a lista");
                });
            }
        });
    }

    private boolean hasMeta() {
        for (Item it : all) if (it.rating > 0 || it.meta > 0 || !it.genres.isEmpty()) return true;
        return false;
    }

    private static String readAll(InputStream in) throws Exception {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(131072);
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toString("UTF-8");
        } finally {
            in.close();
        }
    }

    private static List<Item> parse(String json) throws Exception {
        JSONArray arr = new JSONObject(json).getJSONArray("titles");
        List<Item> out = new ArrayList<>(arr.length());
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            Item it = new Item();
            it.id = o.optString("id");
            it.title = o.optString("title");
            it.year = o.optInt("year", 0);
            it.rating = o.optDouble("rating", 0);
            if (Double.isNaN(it.rating)) it.rating = 0;
            it.meta = o.optInt("metascore", 0);
            String poster = o.optString("poster");
            it.poster = poster.equals("null") ? "" : poster;
            String type = o.optString("type");
            it.series = type.equals("tvSeries") || type.equals("tvMiniSeries");
            it.kind = type.equals("movie") ? 1 : type.equals("tvMovie") ? 2 : it.series ? 3 : 0;
            JSONArray g = o.optJSONArray("genres");
            if (g != null) {
                for (int k = 0; k < g.length(); k++) {
                    String name = g.optString(k);
                    if (name.isEmpty()) continue;
                    String pt = GENRES_PT.get(name);
                    it.genres.add(pt != null ? pt : name);
                }
            }
            if (!it.id.isEmpty() && !it.title.isEmpty()) out.add(it);
        }
        return out;
    }

    private void setItems(List<Item> items) {
        all.clear();
        all.addAll(items);
        genreCounts.clear();
        for (Item it : all) for (String g : it.genres) {
            Integer n = genreCounts.get(g);
            genreCounts.put(g, n == null ? 1 : n + 1);
        }
        List<String> names = new ArrayList<>(genreCounts.keySet());
        Collections.sort(names, (a, b) -> {
            int d = genreCounts.get(b) - genreCounts.get(a);
            return d != 0 ? d : a.compareTo(b);
        });
        genreNames.clear();
        genreNames.add(ALL);
        genreNames.addAll(names);
        if (!genreNames.contains(current)) current = ALL;
        decadeCounts.clear();
        for (Item it : all) {
            int b = bucket(it.year);
            if (b == -2) continue;
            Integer n = decadeCounts.get(b);
            decadeCounts.put(b, n == null ? 1 : n + 1);
        }
        List<Integer> ds = new ArrayList<>(decadeCounts.keySet());
        Collections.sort(ds, Collections.reverseOrder());
        decadeList.clear();
        decadeList.add(-1);
        decadeList.addAll(ds);
        if (currentDecade != -1 && !decadeCounts.containsKey(currentDecade)) currentDecade = -1;
        java.util.Arrays.fill(kindCounts, 0);
        for (Item it : all) kindCounts[it.kind]++;
        if (currentKind != 0 && kindCounts[currentKind] == 0) currentKind = 0;
        rebuildChips();
        filter();
        render();
        if (getCurrentFocus() == null) focusCard(0);
    }

    private void filter() {
        shown.clear();
        for (Item it : all) {
            if (!current.equals(ALL) && !it.genres.contains(current)) continue;
            if (currentDecade != -1 && bucket(it.year) != currentDecade) continue;
            if (currentKind != 0 && it.kind != currentKind) continue;
            shown.add(it);
        }
        if (currentSort == 1) Collections.sort(shown, (x, y) -> Double.compare(y.rating, x.rating));
        else if (currentSort == 2) Collections.sort(shown, (x, y) -> Integer.compare(y.meta, x.meta));
        int pages = pageCount();
        if (page >= pages) page = pages - 1;
        if (page < 0) page = 0;
    }

    private int pageCount() {
        return Math.max(1, (shown.size() + PER_PAGE - 1) / PER_PAGE);
    }

    private void render() {
        int start = page * PER_PAGE;
        int gen = ++generation;
        for (int i = 0; i < PER_PAGE; i++) {
            int at = start + i;
            if (at < shown.size()) {
                Item it = shown.get(at);
                cards[i].setVisibility(View.VISIBLE);
                cards[i].setFocusable(true);
                titles[i].setText(it.title);
                bindRating(ratings[i], it);
                bindMeta(i, it);
                String meta = it.year > 0 ? String.valueOf(it.year) : "";
                if (it.series) meta = meta.isEmpty() ? "Série" : "Série · " + meta;
                else if (it.kind == 2) meta = meta.isEmpty() ? "Filme TV" : "Filme TV · " + meta;
                metas[i].setText(meta);
                genreViews[i].setText(joinGenres(it));
                bindPoster(posters[i], it, gen);
            } else {
                cards[i].setVisibility(View.INVISIBLE);
                cards[i].setFocusable(false);
                posters[i].setTag(null);
                posters[i].setImageDrawable(null);
                metaViews[i].setVisibility(View.GONE);
            }
        }
        pageView.setText(shown.isEmpty() ? "" : (page + 1) + " / " + pageCount());
        emptyView.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
        if (shown.isEmpty() && !all.isEmpty()) emptyView.setText("Nenhum título neste filtro");
        updateHeader();
    }

    private void bindRating(TextView view, Item it) {
        if (it.rating <= 0) {
            view.setVisibility(View.GONE);
            return;
        }
        view.setVisibility(View.VISIBLE);
        String label = "IMDb ";
        String value = String.format(Locale.US, "%.1f", it.rating);
        SpannableStringBuilder sb = new SpannableStringBuilder(label + value);
        sb.setSpan(new ForegroundColorSpan(TEXT_DIM), 0, label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new RelativeSizeSpan(0.8f), 0, label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new ForegroundColorSpan(GOLD), label.length(), sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        view.setText(sb);
    }

    private void bindMeta(int i, Item it) {
        if (it.meta <= 0) {
            metaViews[i].setVisibility(View.GONE);
            return;
        }
        metaBgs[i].setColor(it.meta >= 61 ? 0xFF2E9E3E : it.meta >= 40 ? 0xFFC99A00 : 0xFFC62828);
        metaViews[i].setText(String.valueOf(it.meta));
        metaViews[i].setVisibility(View.VISIBLE);
    }

    private static String joinGenres(Item it) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < it.genres.size() && i < 3; i++) {
            if (i > 0) sb.append(" · ");
            sb.append(it.genres.get(i));
        }
        return sb.toString();
    }

    private void updateHeader() {
        String title = current.equals(ALL) ? "Watchlist" : current;
        if (currentDecade != -1) title += " · " + decadeLabel(currentDecade);
        if (currentKind != 0) title += " · " + kindLabel(currentKind);
        if (currentSort == 1) title += " · nota IMDb";
        else if (currentSort == 2) title += " · Metascore";
        headerTitle.setText(title);
        headerCount.setText(all.isEmpty() ? "" : String.valueOf(shown.size()));
    }

    private void bindPoster(final ImageView view, final Item it, final int gen) {
        view.setTag(it.id);
        Bitmap cached = memory.get(it.id);
        if (cached != null) {
            view.setImageBitmap(cached);
            return;
        }
        view.setImageDrawable(null);
        if (it.poster.isEmpty()) return;
        try {
            pool.execute(() -> {
                if (gen != generation) return;
                final Bitmap b = loadPoster(it);
                if (b == null) return;
                memory.put(it.id, b);
                runOnUiThread(() -> {
                    if (it.id.equals(view.getTag())) view.setImageBitmap(b);
                });
            });
        } catch (RejectedExecutionException ignored) {}
    }

    private Bitmap loadPoster(Item it) {
        File f = new File(posterDir, it.id + ".jpg");
        if (!f.exists() || f.length() == 0) {
            byte[] data = download(sized(it.poster));
            if (data == null) data = download(it.poster);
            if (data == null) return null;
            try {
                FileOutputStream out = new FileOutputStream(f);
                out.write(data);
                out.close();
            } catch (Exception e) {
                return null;
            }
        }
        Bitmap b = decode(f);
        if (b == null) f.delete();
        return b;
    }

    private Bitmap decode(File f) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getAbsolutePath(), o);
        int sample = 1;
        while (o.outWidth / (sample * 2) >= posterW) sample *= 2;
        o.inJustDecodeBounds = false;
        o.inSampleSize = sample;
        o.inPreferredConfig = Bitmap.Config.RGB_565;
        return BitmapFactory.decodeFile(f.getAbsolutePath(), o);
    }

    private static String sized(String url) {
        int i = url.indexOf("._V1_");
        int dot = url.lastIndexOf('.');
        if (i < 0 || dot <= i) return url;
        return url.substring(0, i) + "._V1_QL75_UX260_" + url.substring(dot);
    }

    private static byte[] download(String url) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(8000);
            c.setReadTimeout(12000);
            if (c.getResponseCode() != 200) return null;
            InputStream in = c.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream(32768);
            byte[] buf = new byte[8192];
            int n;
            int total = 0;
            while ((n = in.read(buf)) > 0) {
                total += n;
                if (total > 2000000) return null;
                out.write(buf, 0, n);
            }
            in.close();
            return out.toByteArray();
        } catch (Exception e) {
            return null;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private void focusCard(int i) {
        if (i >= 0 && i < PER_PAGE && cards[i].getVisibility() == View.VISIBLE) cards[i].requestFocus();
    }

    private void focusRow(int row) {
        for (Chip c : chips) {
            if (rowOf(c) == row && isActive(c)) {
                c.view.requestFocus();
                return;
            }
        }
    }

    private void goToPage(int target, int row, boolean lastColumn) {
        page = target;
        render();
        int onPage = Math.min(PER_PAGE, shown.size() - page * PER_PAGE);
        int rowsOnPage = (onPage + COLS - 1) / COLS;
        int r = Math.min(row, rowsOnPage - 1);
        int idx = r * COLS + (lastColumn ? COLS - 1 : 0);
        focusCard(Math.min(idx, onPage - 1));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        View f = getCurrentFocus();
        int idx = f != null && f.getTag() instanceof Integer ? (Integer) f.getTag() : -1;
        boolean onChip = f != null && f.getTag() instanceof Chip;
        boolean hasNext = (page + 1) * PER_PAGE < shown.size();
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (idx >= 0 && idx % COLS == COLS - 1) {
                    if (hasNext) goToPage(page + 1, idx / COLS, false);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (idx >= 0 && idx % COLS == 0) {
                    if (page > 0) goToPage(page - 1, idx / COLS, true);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                if (idx >= 0 && idx < COLS) {
                    focusRow(2);
                    return true;
                }
                if (onChip) {
                    int r = rowOf((Chip) f.getTag());
                    if (r > 0) focusRow(r - 1);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (onChip) {
                    int r = rowOf((Chip) f.getTag());
                    if (r < 2) focusRow(r + 1);
                    else focusCard(0);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_CHANNEL_DOWN:
            case KeyEvent.KEYCODE_PAGE_DOWN:
            case KeyEvent.KEYCODE_MEDIA_NEXT:
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                if (hasNext) goToPage(page + 1, 0, false);
                return true;
            case KeyEvent.KEYCODE_CHANNEL_UP:
            case KeyEvent.KEYCODE_PAGE_UP:
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                if (page > 0) goToPage(page - 1, 0, false);
                return true;
            case KeyEvent.KEYCODE_MENU:
                focusRow(0);
                return true;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void openInStremio(Item it) {
        String uri = it.series
                ? "stremio:///detail/series/" + it.id
                : "stremio:///detail/movie/" + it.id + "/" + it.id;
        Intent view = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        view.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(view);
            return;
        } catch (Exception ignored) {}
        Intent launch = stremioLaunch();
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(launch);
                return;
            } catch (Exception ignored) {}
        }
        Toast.makeText(this, "Stremio não encontrado", Toast.LENGTH_LONG).show();
    }

    private Intent stremioLaunch() {
        PackageManager pm = getPackageManager();
        String[] categories = {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER};
        for (String category : categories) {
            Intent probe = new Intent(Intent.ACTION_MAIN).addCategory(category);
            for (ResolveInfo r : pm.queryIntentActivities(probe, 0)) {
                CharSequence label = r.loadLabel(pm);
                if (label != null && label.toString().toLowerCase().contains("stremio")) {
                    Intent i = pm.getLaunchIntentForPackage(r.activityInfo.packageName);
                    if (i != null) return i;
                }
            }
        }
        return null;
    }
}
