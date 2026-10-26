package com.ansub.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
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
    private static final int MENU_BG = 0xFFEDEAF5;
    private static final int MENU_TEXT = 0xFF1A1426;
    private static final int MENU_DIM = 0xFF5B5470;
    private static final int MENU_FOCUS = 0xFFC9BDEB;
    private static final int MENU_SELECTED = 0xFFDCD3F2;
    private static final double[] MIN_RATINGS = {6.0, 7.0, 8.0};
    private static final long NEW_MS = 14L * 24L * 3600L * 1000L;
    private static final int M_ALL = 0, M_NEW = 1, M_SOON = 2, M_TOPM = 3, M_TOPS = 4;

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
        int runtime;
        String release = "";
        String plot = "";
        List<String> directors = new ArrayList<>();
        List<String> cast = new ArrayList<>();
        boolean upcoming;
        long addedAt;
        String norm = "";
        List<String> genres = new ArrayList<>();
    }

    private static final int T_ALL = -1, T_GENRE = 0, T_YEAR = 1, T_KIND = 2, T_SORT = 3, T_MORE = 4, T_DUR = 5;
    private static final int T_NEW = 10, T_SOON = 11, T_TOPM = 13, T_RANDOM = 14, T_SEARCH = 15, T_TOPS = 16;

    private static final class Drop {
        int type;
        int row;
        TextView view;
        GradientDrawable bg;
    }

    private static final class Option {
        String label;
        int count = -1;
        boolean selected;
        boolean keepOpen;
        Runnable apply;
    }

    private static final class Chevron extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int size;

        Chevron(int size, int color) {
            this.size = size;
            paint.setColor(color);
            paint.setStyle(Paint.Style.FILL);
        }

        @Override
        public void draw(Canvas canvas) {
            Path path = new Path();
            float w = getBounds().width();
            float h = getBounds().height();
            path.moveTo(0, h * 0.30f);
            path.lineTo(w, h * 0.30f);
            path.lineTo(w / 2f, h * 0.75f);
            path.close();
            canvas.drawPath(path, paint);
        }

        @Override
        public int getIntrinsicWidth() {
            return size;
        }

        @Override
        public int getIntrinsicHeight() {
            return size;
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter filter) {
            paint.setColorFilter(filter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    private final List<Item> all = new ArrayList<>();
    private final List<Item> shown = new ArrayList<>();
    private final List<String> genreNames = new ArrayList<>();
    private final Map<String, Integer> genreCounts = new HashMap<>();
    private final List<Drop> bar = new ArrayList<>();
    private final Set<String> selGenres = new LinkedHashSet<>();
    private final List<Item> topMovies = new ArrayList<>();
    private final List<Item> topSeries = new ArrayList<>();
    private int mode = M_ALL;
    private int currentDur = 0;
    private String query = "";
    private String[] queryTokens = new String[0];
    private final Random random = new Random();
    private Drop searchDrop;
    private LinearLayout tabBar;
    private int currentDecade = -1;
    private int currentKind = 0;
    private int currentSort = 0;
    private double minRating = 0;
    private final int[] kindCounts = new int[4];
    private final Map<Integer, Integer> decadeCounts = new HashMap<>();
    private final List<Integer> decadeList = new ArrayList<>();
    private String lastRaw = "";
    private int page = 0;
    private volatile int generation = 0;
    private int posterW;
    private int lastBar = 0;
    private FrameLayout root;
    private LinearLayout filterBar;
    private ScrollView menu;
    private LinearLayout menuList;
    private Drop openDrop;

    private final FrameLayout[] cards = new FrameLayout[PER_PAGE];
    private final ImageView[] posters = new ImageView[PER_PAGE];
    private final TextView[] titles = new TextView[PER_PAGE];
    private final TextView[] ratings = new TextView[PER_PAGE];
    private final TextView[] metaViews = new TextView[PER_PAGE];
    private final TextView[] tagViews = new TextView[PER_PAGE];
    private final GradientDrawable[] tagBgs = new GradientDrawable[PER_PAGE];
    private final GradientDrawable[] metaBgs = new GradientDrawable[PER_PAGE];
    private final TextView[] metas = new TextView[PER_PAGE];
    private final TextView[] genreViews = new TextView[PER_PAGE];

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
        int cellH = (hdp - 210) / ROWS;
        int pw = Math.min((cellH - 16) * 2 / 3, cellW * 52 / 100);
        posterW = dp(pw);

        root = new FrameLayout(this);

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

        tabBar = new LinearLayout(this);
        tabBar.setOrientation(LinearLayout.HORIZONTAL);
        tabBar.setGravity(Gravity.CENTER_VERTICAL);
        tabBar.setPadding(dp(4), dp(3), dp(4), dp(3));
        addDrop(T_ALL, tabBar, 0);
        addDrop(T_NEW, tabBar, 0);
        addDrop(T_SOON, tabBar, 0);
        addDrop(T_TOPM, tabBar, 0);
        addDrop(T_TOPS, tabBar, 0);
        tabBar.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));
        addDrop(T_RANDOM, tabBar, 0);
        addDrop(T_SEARCH, tabBar, 0);
        LinearLayout.LayoutParams tabLp = new LinearLayout.LayoutParams(-1, dp(44));
        tabLp.topMargin = dp(2);
        main.addView(tabBar, tabLp);

        filterBar = new LinearLayout(this);
        filterBar.setOrientation(LinearLayout.HORIZONTAL);
        filterBar.setGravity(Gravity.CENTER_VERTICAL);
        filterBar.setPadding(dp(4), dp(3), dp(4), dp(3));
        addDrop(T_GENRE, filterBar, 1);
        addDrop(T_YEAR, filterBar, 1);
        addDrop(T_KIND, filterBar, 1);
        addDrop(T_DUR, filterBar, 1);
        addDrop(T_SORT, filterBar, 1);
        addDrop(T_MORE, filterBar, 1);
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(-1, dp(44));
        barLp.bottomMargin = dp(8);
        main.addView(filterBar, barLp);

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
            if (at < shown.size()) showDetails(shown.get(at));
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
        TextView tagView = text(9, TEXT, Typeface.BOLD);
        GradientDrawable tagBg = rounded(0xFF7E57C2, 4);
        tagView.setBackground(tagBg);
        tagView.setGravity(Gravity.CENTER);
        tagView.setSingleLine(true);
        tagView.setPadding(dp(3), dp(1), dp(3), dp(1));
        tagView.setVisibility(View.GONE);
        FrameLayout.LayoutParams tagLp = new FrameLayout.LayoutParams(-2, -2, Gravity.START | Gravity.BOTTOM);
        tagLp.setMargins(dp(3), 0, 0, dp(3));
        posterBox.addView(tagView, tagLp);
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
        tagViews[index] = tagView;
        tagBgs[index] = tagBg;
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

    private void addDrop(int type, LinearLayout row, int rowIndex) {
        final Drop d = new Drop();
        d.type = type;
        d.row = rowIndex;
        d.bg = rounded(CARD, 20);
        TextView v = text(15, TEXT_DIM, Typeface.NORMAL);
        v.setSingleLine(true);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(16), 0, dp(16), 0);
        v.setBackground(d.bg);
        v.setFocusable(true);
        v.setClickable(true);
        v.setTag(d);
        if (type >= T_GENRE && type <= T_DUR) {
            Chevron chevron = new Chevron(dp(14), 0xCCFFFFFF);
            chevron.setBounds(0, 0, dp(14), dp(14));
            v.setCompoundDrawables(null, null, chevron, null);
            v.setCompoundDrawablePadding(dp(8));
        }
        v.setOnFocusChangeListener((view, has) -> {
            if (has) lastBar = bar.indexOf(d);
            styleDrop(d, has);
        });
        v.setOnClickListener(view -> onBarClick(d));
        d.view = v;
        if (type == T_SEARCH) searchDrop = d;
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -1);
        lp.rightMargin = dp(10);
        row.addView(v, lp);
        bar.add(d);
    }

    private void onBarClick(Drop d) {
        switch (d.type) {
            case T_ALL:
                clearFilters();
                applyChange(null);
                break;
            case T_NEW:
                toggleMode(M_NEW, d);
                break;
            case T_SOON:
                toggleMode(M_SOON, d);
                break;
            case T_TOPM:
                toggleMode(M_TOPM, d);
                break;
            case T_TOPS:
                toggleMode(M_TOPS, d);
                break;
            case T_RANDOM:
                pickRandom();
                break;
            case T_SEARCH:
                openSearch();
                break;
            default:
                openMenu(d);
        }
    }

    private void toggleMode(int target, Drop d) {
        mode = mode == target ? M_ALL : target;
        applyChange(d);
    }

    private void clearFilters() {
        selGenres.clear();
        currentDecade = -1;
        currentKind = 0;
        currentSort = 0;
        minRating = 0;
        currentDur = 0;
        query = "";
        queryTokens = new String[0];
        mode = M_ALL;
    }

    private void applyChange(Drop focusOn) {
        page = 0;
        refreshBar();
        filter();
        render();
        if (focusOn != null) focusOn.view.requestFocus();
        else focusCard(0);
    }

    private boolean isDefault() {
        return mode == M_ALL && selGenres.isEmpty() && currentDecade == -1 && currentKind == 0
                && currentSort == 0 && minRating <= 0 && currentDur == 0 && query.isEmpty();
    }

    private boolean isActive(Drop d) {
        switch (d.type) {
            case T_GENRE: return !selGenres.isEmpty();
            case T_YEAR: return currentDecade != -1;
            case T_KIND: return currentKind != 0;
            case T_SORT: return currentSort != 0;
            case T_MORE: return minRating > 0;
            case T_DUR: return currentDur != 0;
            case T_NEW: return mode == M_NEW;
            case T_SOON: return mode == M_SOON;
            case T_TOPM: return mode == M_TOPM;
            case T_TOPS: return mode == M_TOPS;
            case T_SEARCH: return !query.isEmpty();
            case T_RANDOM: return false;
            default: return isDefault();
        }
    }

    private String dropLabel(Drop d) {
        switch (d.type) {
            case T_GENRE:
                if (selGenres.isEmpty()) return "Gêneros";
                String first = selGenres.iterator().next();
                return selGenres.size() == 1 ? first : first + " +" + (selGenres.size() - 1);
            case T_YEAR: return currentDecade == -1 ? "Anos" : decadeLabel(currentDecade);
            case T_KIND: return currentKind == 0 ? "Tipos" : kindLabel(currentKind);
            case T_DUR: return currentDur == 0 ? "Duração" : durLabel(currentDur);
            case T_SORT: return currentSort == 1 ? "Maior nota IMDb" : currentSort == 2 ? "Maior Metascore" : "Ordem";
            case T_MORE: return minRating > 0 ? ratingLabel(minRating) : "Mais";
            case T_NEW: return "Novos " + countNew();
            case T_SOON: return "Em breve " + countSoon();
            case T_TOPM: return "Top 10 Filmes";
            case T_TOPS: return "Top 10 Séries";
            case T_RANDOM: return "Sortear";
            case T_SEARCH:
                if (query.isEmpty()) return "Buscar";
                return query.length() > 14 ? "“" + query.substring(0, 14) + "…”" : "“" + query + "”";
            default: return all.isEmpty() ? "Todos" : "Todos  " + all.size();
        }
    }

    private static String ratingLabel(double r) {
        return "IMDb " + String.format(Locale.US, "%.1f", r) + "+";
    }

    private static String durLabel(int d) {
        return d == 1 ? "Até 1h30" : d == 2 ? "1h30 a 2h" : d == 3 ? "2h a 2h30" : "Mais de 2h30";
    }

    private static boolean durFits(Item it, int d) {
        if (it.kind == 3 || it.runtime <= 0) return false;
        switch (d) {
            case 1: return it.runtime <= 90;
            case 2: return it.runtime > 90 && it.runtime <= 120;
            case 3: return it.runtime > 120 && it.runtime <= 150;
            default: return it.runtime > 150;
        }
    }

    private static String releaseLabel(String r) {
        if (r == null || r.isEmpty()) return "";
        String[] p = r.split("-");
        if (p.length == 3) return p[2] + "/" + p[1] + "/" + p[0];
        if (p.length == 2) return p[1] + "/" + p[0];
        return p[0];
    }

    private static String normalize(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
    }

    private static String[] tokens(String q) {
        String n = normalize(q).trim();
        return n.isEmpty() ? new String[0] : n.split("\\s+");
    }

    // NOVO: entrou na Watchlist nos últimos 14 dias e já foi lançado.
    private boolean inTop() {
        return mode == M_TOPM || mode == M_TOPS;
    }

    private boolean isNew(Item it) {
        return !it.upcoming && it.addedAt > 0 && System.currentTimeMillis() - it.addedAt <= NEW_MS;
    }

    private int countNew() {
        int n = 0;
        for (Item it : all) if (isNew(it)) n++;
        return n;
    }

    private int countSoon() {
        int n = 0;
        for (Item it : all) if (it.upcoming) n++;
        return n;
    }

    private boolean matches(Item it, int skip) {
        if (skip != T_GENRE) for (String g : selGenres) if (!it.genres.contains(g)) return false;
        if (skip != T_YEAR && currentDecade != -1 && bucket(it.year) != currentDecade) return false;
        if (skip != T_KIND && currentKind != 0 && it.kind != currentKind) return false;
        if (skip != T_MORE && minRating > 0 && it.rating < minRating) return false;
        if (skip != T_DUR && currentDur != 0 && !durFits(it, currentDur)) return false;
        for (String token : queryTokens) if (!it.norm.contains(token)) return false;
        if (mode == M_NEW && !isNew(it)) return false;
        if (mode == M_SOON && !it.upcoming) return false;
        return true;
    }

    private Option option(String label, int count, boolean selected, Runnable apply) {
        Option o = new Option();
        o.label = label;
        o.count = count;
        o.selected = selected;
        o.apply = apply;
        return o;
    }

    private List<Option> optionsFor(int type) {
        List<Option> out = new ArrayList<>();
        switch (type) {
            case T_GENRE: {
                int total = 0;
                for (Item it : all) if (matches(it, T_GENRE)) total++;
                Option clear = option("Todos os gêneros", total, selGenres.isEmpty(), selGenres::clear);
                clear.keepOpen = true;
                out.add(clear);
                for (final String g : genreNames) {
                    if (g.equals(ALL)) continue;
                    int n = 0;
                    for (Item it : all) if (matches(it, -2) && it.genres.contains(g)) n++;
                    boolean on = selGenres.contains(g);
                    if (n == 0 && !on) continue;
                    Option o = option(g, n, on, () -> {
                        if (!selGenres.remove(g)) selGenres.add(g);
                    });
                    o.keepOpen = true;
                    out.add(o);
                }
                break;
            }
            case T_YEAR: {
                int total = 0;
                for (Item it : all) if (matches(it, T_YEAR)) total++;
                out.add(option("Todos os anos", total, currentDecade == -1, () -> currentDecade = -1));
                for (final int d : decadeList) {
                    if (d == -1) continue;
                    int n = 0;
                    for (Item it : all) if (matches(it, T_YEAR) && bucket(it.year) == d) n++;
                    if (n == 0 && d != currentDecade) continue;
                    out.add(option(decadeLabel(d), n, d == currentDecade, () -> currentDecade = d));
                }
                break;
            }
            case T_KIND: {
                int total = 0;
                for (Item it : all) if (matches(it, T_KIND)) total++;
                out.add(option("Todos os tipos", total, currentKind == 0, () -> currentKind = 0));
                for (int k = 1; k <= 3; k++) {
                    final int kind = k;
                    int n = 0;
                    for (Item it : all) if (matches(it, T_KIND) && it.kind == kind) n++;
                    if (n == 0 && kind != currentKind) continue;
                    out.add(option(kindLabel(kind), n, kind == currentKind, () -> currentKind = kind));
                }
                break;
            }
            case T_DUR: {
                int total = 0;
                for (Item it : all) if (matches(it, T_DUR)) total++;
                out.add(option("Qualquer duração", total, currentDur == 0, () -> currentDur = 0));
                for (int k = 1; k <= 4; k++) {
                    final int dur = k;
                    int n = 0;
                    for (Item it : all) if (matches(it, T_DUR) && durFits(it, dur)) n++;
                    if (n == 0 && dur != currentDur) continue;
                    out.add(option(durLabel(dur) + " (filmes)", n, dur == currentDur, () -> currentDur = dur));
                }
                break;
            }
            case T_SORT:
                out.add(option("Ordem padrão", -1, currentSort == 0, () -> currentSort = 0));
                out.add(option("Maior nota IMDb", -1, currentSort == 1, () -> currentSort = 1));
                out.add(option("Maior Metascore", -1, currentSort == 2, () -> currentSort = 2));
                break;
            default: {
                int total = 0;
                for (Item it : all) if (matches(it, T_MORE)) total++;
                out.add(option("Qualquer nota", total, minRating <= 0, () -> minRating = 0));
                for (final double r : MIN_RATINGS) {
                    int n = 0;
                    for (Item it : all) if (matches(it, T_MORE) && it.rating >= r) n++;
                    if (n == 0 && r != minRating) continue;
                    out.add(option("Nota IMDb " + String.format(Locale.US, "%.1f", r) + " ou mais", n,
                            r == minRating, () -> minRating = r));
                }
                if (!isDefault()) {
                    out.add(option("Limpar todos os filtros", -1, false, this::clearFilters));
                }
                break;
            }
        }
        return out;
    }

    private void openMenu(Drop d) {
        if (inTop()) {
            mode = M_ALL;
            page = 0;
            refreshBar();
            filter();
            render();
        }
        showMenu(d, -1);
    }

    private void showMenu(final Drop d, int focusIdx) {
        closeMenu(false);
        List<Option> options = optionsFor(d.type);
        if (options.isEmpty()) return;
        openDrop = d;
        menuList = new LinearLayout(this);
        menuList.setOrientation(LinearLayout.VERTICAL);
        menuList.setPadding(dp(6), dp(6), dp(6), dp(6));
        View selected = null;
        for (final Option o : options) {
            final GradientDrawable obg = rounded(o.selected ? MENU_SELECTED : 0, 8);
            TextView t = text(16, MENU_TEXT, o.selected ? Typeface.BOLD : Typeface.NORMAL);
            t.setText(o.count >= 0 ? o.label + " (" + o.count + ")" : o.label);
            t.setSingleLine(true);
            t.setEllipsize(TextUtils.TruncateAt.END);
            t.setPadding(dp(16), dp(11), dp(16), dp(11));
            t.setBackground(obg);
            t.setFocusable(true);
            t.setClickable(true);
            t.setOnFocusChangeListener((v, has) -> obg.setColor(has ? MENU_FOCUS : o.selected ? MENU_SELECTED : 0));
            t.setOnClickListener(v -> {
                int idx = menuList.indexOfChild(v);
                o.apply.run();
                if (o.keepOpen) {
                    page = 0;
                    refreshBar();
                    filter();
                    render();
                    showMenu(d, idx);
                } else {
                    closeMenu(false);
                    applyChange(d);
                }
            });
            menuList.addView(t, new LinearLayout.LayoutParams(-1, -2));
            if (selected == null && o.selected) selected = t;
        }
        View target = selected != null ? selected : menuList.getChildAt(0);
        if (focusIdx >= 0) target = menuList.getChildAt(Math.min(focusIdx, menuList.getChildCount() - 1));
        int width = dp(300);
        int[] at = new int[2];
        int[] origin = new int[2];
        d.view.getLocationInWindow(at);
        root.getLocationInWindow(origin);
        int x = Math.max(dp(8), Math.min(at[0] - origin[0], root.getWidth() - width - dp(8)));
        int y = at[1] - origin[1] + d.view.getHeight() + dp(6);
        int maxH = Math.max(dp(120), root.getHeight() - y - dp(16));
        menuList.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int height = Math.min(menuList.getMeasuredHeight(), maxH);
        menu = new ScrollView(this);
        menu.setBackground(rounded(MENU_BG, 14));
        menu.setClipToOutline(true);
        menu.setOverScrollMode(View.OVER_SCROLL_NEVER);
        menu.addView(menuList, new FrameLayout.LayoutParams(-1, -2));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(width, height, Gravity.START | Gravity.TOP);
        lp.leftMargin = x;
        lp.topMargin = y;
        root.addView(menu, lp);
        if (target != null) target.requestFocus();
    }

    private void closeMenu(boolean restoreFocus) {
        if (menu != null) root.removeView(menu);
        menu = null;
        menuList = null;
        Drop d = openDrop;
        openDrop = null;
        if (restoreFocus && d != null) d.view.requestFocus();
    }

    @Override
    public void onBackPressed() {
        if (openDrop != null) {
            closeMenu(true);
            return;
        }
        super.onBackPressed();
    }

    private void pickRandom() {
        if (shown.isEmpty()) {
            Toast.makeText(this, "Nenhum título para sortear neste filtro", Toast.LENGTH_SHORT).show();
            return;
        }
        int at = random.nextInt(shown.size());
        Item it = shown.get(at);
        page = at / PER_PAGE;
        render();
        focusCard(at % PER_PAGE);
        String extra = it.rating > 0 ? " · IMDb " + String.format(Locale.US, "%.1f", it.rating) : "";
        Toast.makeText(this, "Sorteado: " + it.title + extra, Toast.LENGTH_LONG).show();
    }

    // Busca digitada (teclado na tela / teclado USB); não usa o microfone do controle.
    private void openSearch() {
        closeMenu(false);
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        final EditText input = new EditText(builder.getContext());
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setText(query);
        input.setSelection(input.getText().length());
        input.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        builder.setTitle("Buscar título");
        builder.setPositiveButton("Buscar", (dialog, which) -> applySearch(input.getText().toString()));
        builder.setNegativeButton("Limpar", (dialog, which) -> applySearch(""));
        final AlertDialog dialog = builder.create();
        final LinearLayout box = new LinearLayout(builder.getContext());
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(input, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        final LinearLayout suggestions = new LinearLayout(builder.getContext());
        suggestions.setOrientation(LinearLayout.VERTICAL);
        box.addView(suggestions, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        dialog.setView(box, dp(24), dp(8), dp(24), dp(8));
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                fillSuggestions(suggestions, s.toString(), dialog);
            }
        });
        fillSuggestions(suggestions, query, dialog);
        input.setOnEditorActionListener((v, actionId, event) -> {
            applySearch(input.getText().toString());
            dialog.dismiss();
            return true;
        });
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        dialog.show();
        input.requestFocus();
    }

    private void fillSuggestions(LinearLayout box, String text, final AlertDialog dialog) {
        box.removeAllViews();
        String[] tk = tokens(text);
        if (tk.length == 0) return;
        List<Item> hits = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (int pass = 0; pass < 2 && hits.size() < 5; pass++) {
            for (Item it : all) {
                if (hits.size() >= 5) break;
                boolean ok = true;
                for (String t : tk) {
                    if (!it.norm.contains(t)) {
                        ok = false;
                        break;
                    }
                }
                if (!ok) continue;
                if ((pass == 0) != it.norm.startsWith(tk[0])) continue;
                if (seen.add(it.norm)) hits.add(it);
            }
        }
        for (final Item it : hits) {
            final TextView row = new TextView(box.getContext());
            row.setText(it.year > 0 ? it.title + " (" + it.year + ")" : it.title);
            row.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
            row.setTextColor(TEXT);
            row.setSingleLine(true);
            row.setEllipsize(TextUtils.TruncateAt.END);
            row.setPadding(dp(12), dp(10), dp(12), dp(10));
            row.setFocusable(true);
            row.setClickable(true);
            row.setOnFocusChangeListener((v, focused) -> {
                row.setBackground(focused ? rounded(MENU_FOCUS, 6) : null);
                row.setTextColor(focused ? MENU_TEXT : TEXT);
            });
            row.setOnClickListener(v -> {
                applySearch(it.title);
                dialog.dismiss();
            });
            box.addView(row, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
    }

    private void applySearch(String text) {
        query = text.trim();
        queryTokens = tokens(query);
        if (inTop()) mode = M_ALL;
        applyChange(searchDrop);
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

    private void refreshBar() {
        for (Drop d : bar) {
            d.view.setText(dropLabel(d));
            styleDrop(d, d.view.hasFocus());
        }
    }

    private void styleDrop(Drop d, boolean focused) {
        boolean active = isActive(d);
        d.bg.setColor(focused ? CARD_ON : active ? 0x33FFFFFF : CARD);
        d.bg.setStroke(focused ? dp(2) : 0, 0xFFFFFFFF);
        d.view.setTextColor(active || focused ? TEXT : TEXT_DIM);
        d.view.setTypeface(Typeface.SANS_SERIF, active ? Typeface.BOLD : Typeface.NORMAL);
    }

    private void loadCache() {
        File f = new File(getFilesDir(), CACHE_FILE);
        if (!f.exists()) return;
        try {
            String raw = readAll(new FileInputStream(f));
            List<Item> parsed = parse(raw);
            if (!parsed.isEmpty()) {
                lastRaw = raw;
                loadTop(raw);
                setItems(parsed);
            }
        } catch (Exception ignored) {}
    }

    private void refreshIfStale() {
        File f = new File(getFilesDir(), CACHE_FILE);
        // Cache antigo sem gênero/nota/sinopse (JSON de antes da atualização) é refeito na hora, sem esperar os 30 min.
        if (f.exists() && !all.isEmpty() && hasMeta() && lastRaw.contains("\"topMovies\"") && lastRaw.contains("\"plot\"")
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
                    loadTop(raw);
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
            Item it = readItem(arr.getJSONObject(i));
            if (it != null) out.add(it);
        }
        return out;
    }

    private static List<Item> parseTop(String json, String key) {
        List<Item> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONObject(json).optJSONArray(key);
            if (arr == null) return out;
            for (int i = 0; i < arr.length() && out.size() < 10; i++) {
                Item it = readItem(arr.getJSONObject(i));
                if (it != null) out.add(it);
            }
        } catch (Exception ignored) {}
        return out;
    }

    private static Item readItem(JSONObject o) {
        Item it = new Item();
        it.id = o.optString("id");
        it.title = o.optString("title");
        it.norm = normalize(it.title);
        it.year = o.optInt("year", 0);
        it.rating = o.optDouble("rating", 0);
        if (Double.isNaN(it.rating)) it.rating = 0;
        it.meta = o.optInt("metascore", 0);
        String poster = o.optString("poster");
        it.poster = poster.equals("null") ? "" : poster;
        String type = o.optString("type");
        it.series = type.equals("tvSeries") || type.equals("tvMiniSeries");
        it.kind = type.equals("movie") ? 1 : type.equals("tvMovie") ? 2 : it.series ? 3 : 0;
        it.runtime = o.optInt("runtime", 0);
        it.release = o.optString("release", "");
        it.upcoming = o.optBoolean("upcoming", false);
        it.plot = o.optString("plot", "");
        addNames(it.directors, o.optJSONArray("directors"));
        addNames(it.cast, o.optJSONArray("cast"));
        it.addedAt = o.optLong("addedAt", 0L) * 1000L;
        JSONArray g = o.optJSONArray("genres");
        if (g != null) {
            for (int k = 0; k < g.length(); k++) {
                String name = g.optString(k);
                if (name.isEmpty()) continue;
                String pt = GENRES_PT.get(name);
                it.genres.add(pt != null ? pt : name);
            }
        }
        return !it.id.isEmpty() && !it.title.isEmpty() ? it : null;
    }

    private static void addNames(List<String> out, JSONArray arr) {
        if (arr == null) return;
        for (int k = 0; k < arr.length(); k++) {
            String n = arr.optString(k);
            if (!n.isEmpty()) out.add(n);
        }
    }

    private void loadTop(String raw) {
        topMovies.clear();
        topMovies.addAll(parseTop(raw, "topMovies"));
        topSeries.clear();
        topSeries.addAll(parseTop(raw, "topSeries"));
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
        selGenres.retainAll(genreNames);
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
        if (minRating > 0 && all.isEmpty()) minRating = 0;
        refreshBar();
        filter();
        render();
        if (getCurrentFocus() == null) focusCard(0);
    }

    private void filter() {
        shown.clear();
        if (inTop()) {
            shown.addAll(mode == M_TOPM ? topMovies : topSeries);
        } else {
            for (Item it : all) if (matches(it, -2)) shown.add(it);
            if (currentSort == 1) Collections.sort(shown, (x, y) -> Double.compare(y.rating, x.rating));
            else if (currentSort == 2) Collections.sort(shown, (x, y) -> Integer.compare(y.meta, x.meta));
        }
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
                titles[i].setText(inTop() ? (at + 1) + ". " + it.title : it.title);
                bindRating(ratings[i], it);
                bindMeta(i, it);
                bindTag(i, it);
                String meta = it.year > 0 ? String.valueOf(it.year) : "";
                if (it.upcoming && !inTop() && !it.release.isEmpty()) meta = "Estreia " + releaseLabel(it.release);
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
                tagViews[i].setVisibility(View.GONE);
            }
        }
        pageView.setText(shown.isEmpty() ? "" : (page + 1) + " / " + pageCount());
        emptyView.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
        if (shown.isEmpty() && !all.isEmpty()) emptyView.setText(inTop() ? "Top 10 indisponível no momento" : "Nenhum título neste filtro");
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

    // Dois selos: EM BREVE (ainda não lançado) e NOVO (entrou na lista há pouco e já foi lançado).
    private void bindTag(int i, Item it) {
        String label = null;
        int color = 0;
        if (!inTop()) {
            if (it.upcoming) {
                label = "EM BREVE";
                color = 0xFF1E6FD9;
            } else if (isNew(it)) {
                label = "NOVO";
                color = 0xFF7E57C2;
            }
        }
        if (label == null) {
            tagViews[i].setVisibility(View.GONE);
            return;
        }
        tagBgs[i].setColor(color);
        tagViews[i].setText(label);
        tagViews[i].setVisibility(View.VISIBLE);
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
        String title = "Watchlist";
        if (inTop()) {
            title = mode == M_TOPM ? "Top 10 filmes mais populares" : "Top 10 séries mais populares";
        } else {
            if (!selGenres.isEmpty()) title = TextUtils.join(" + ", selGenres);
            if (mode == M_NEW) title += " · novos";
            else if (mode == M_SOON) title += " · em breve";
            if (currentDecade != -1) title += " · " + decadeLabel(currentDecade);
            if (currentKind != 0) title += " · " + kindLabel(currentKind);
            if (currentDur != 0) title += " · " + durLabel(currentDur);
            if (minRating > 0) title += " · " + ratingLabel(minRating);
            if (!query.isEmpty()) title += " · “" + query + "”";
            if (currentSort == 1) title += " · nota IMDb";
            else if (currentSort == 2) title += " · Metascore";
        }
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

    private void focusBar() {
        if (bar.isEmpty()) return;
        bar.get(Math.max(0, Math.min(lastBar, bar.size() - 1))).view.requestFocus();
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
        if (openDrop != null && menuList != null) {
            int at = menuList.indexOfChild(getCurrentFocus());
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    return at <= 0 || super.onKeyDown(keyCode, event);
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    return at == menuList.getChildCount() - 1 || super.onKeyDown(keyCode, event);
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                case KeyEvent.KEYCODE_CHANNEL_DOWN:
                case KeyEvent.KEYCODE_CHANNEL_UP:
                case KeyEvent.KEYCODE_PAGE_DOWN:
                case KeyEvent.KEYCODE_PAGE_UP:
                case KeyEvent.KEYCODE_MENU:
                    return true;
                default:
                    return super.onKeyDown(keyCode, event);
            }
        }
        View f = getCurrentFocus();
        int idx = f != null && f.getTag() instanceof Integer ? (Integer) f.getTag() : -1;
        Drop chip = f != null && f.getTag() instanceof Drop ? (Drop) f.getTag() : null;
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
                    focusBar();
                    return true;
                }
                if (chip != null && chip.row == 0) return true;
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (chip != null && chip.row == 1) {
                    focusCard(0);
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
                focusBar();
                return true;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    // Popup de detalhes: dados já vêm no JSON (nenhum pedido de rede ao abrir).
    private void showDetails(final Item it) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(8), dp(4), dp(8), dp(4));

        TextView title = text(24, TEXT, Typeface.BOLD);
        title.setText(it.title);
        box.addView(title);

        StringBuilder info = new StringBuilder();
        if (it.year > 0) info.append(it.year);
        if (it.runtime > 0) info.append(info.length() > 0 ? " · " : "").append(it.runtime).append(" min");
        if (it.rating > 0) info.append(info.length() > 0 ? " · " : "").append("IMDb ").append(String.format(Locale.US, "%.1f", it.rating));
        if (it.meta > 0) info.append(info.length() > 0 ? " · " : "").append("Metascore ").append(it.meta);
        if (!it.genres.isEmpty()) info.append(info.length() > 0 ? " · " : "").append(TextUtils.join(", ", it.genres));
        if (info.length() > 0) {
            TextView meta = text(14, TEXT_DIM, Typeface.NORMAL);
            meta.setText(info.toString());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.topMargin = dp(6);
            box.addView(meta, lp);
        }
        addDetailLine(box, "Direção: ", TextUtils.join(", ", it.directors));
        addDetailLine(box, "Elenco: ", TextUtils.join(", ", it.cast));

        TextView plot = text(16, TEXT, Typeface.NORMAL);
        plot.setLineSpacing(0, 1.15f);
        plot.setText(it.plot.isEmpty() ? "Sem sinopse disponível." : it.plot);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(plot);
        LinearLayout.LayoutParams plotLp = new LinearLayout.LayoutParams(-1, -2);
        plotLp.topMargin = dp(12);
        box.addView(scroll, plotLp);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams btnRowLp = new LinearLayout.LayoutParams(-1, -2);
        btnRowLp.topMargin = dp(16);
        box.addView(buttons, btnRowLp);

        final AlertDialog dialog = builder.create();
        TextView open = detailButton("Abrir no Stremio", () -> {
            dialog.dismiss();
            openInStremio(it);
        });
        buttons.addView(open);
        buttons.addView(detailButton("Trailer", () -> openTrailer(it)), buttonLp());
        buttons.addView(detailButton("Fechar", dialog::dismiss), buttonLp());

        dialog.setView(box, dp(20), dp(16), dp(20), dp(12));
        dialog.show();
        open.requestFocus();
    }

    private void addDetailLine(LinearLayout box, String label, String value) {
        if (value.isEmpty()) return;
        TextView t = text(14, TEXT, Typeface.NORMAL);
        SpannableStringBuilder sb = new SpannableStringBuilder(label + value);
        sb.setSpan(new ForegroundColorSpan(TEXT_DIM), 0, label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        t.setText(sb);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.topMargin = dp(6);
        box.addView(t, lp);
    }

    private LinearLayout.LayoutParams buttonLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.leftMargin = dp(10);
        return lp;
    }

    private TextView detailButton(String label, final Runnable action) {
        final TextView b = text(16, TEXT, Typeface.BOLD);
        final GradientDrawable bg = rounded(CARD, 8);
        b.setText(label);
        b.setBackground(bg);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(16), dp(10), dp(16), dp(10));
        b.setFocusable(true);
        b.setClickable(true);
        b.setOnFocusChangeListener((v, has) -> {
            bg.setColor(has ? CARD_ON : CARD);
            bg.setStroke(has ? dp(2) : 0, 0xFFFFFFFF);
        });
        b.setOnClickListener(v -> action.run());
        return b;
    }

    // Abre o app do YouTube da TV numa busca pelo trailer (sem tocar vídeo dentro do app).
    private void openTrailer(Item it) {
        String q = it.title + (it.year > 0 ? " " + it.year : "") + " trailer";
        Intent view = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(q)));
        view.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(view);
        } catch (Exception e) {
            Toast.makeText(this, "YouTube não encontrado nesta TV", Toast.LENGTH_LONG).show();
        }
    }

    private void openInStremio(Item it) {
        String uri = it.series
                ? "stremio:///detail/series/" + it.id
                : "stremio:///detail/movie/" + it.id + "/" + it.id;
        Intent view = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        view.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (view.resolveActivity(getPackageManager()) != null) {
            StremioReset.start(this, view);
            return;
        }
        Intent launch = stremioLaunch();
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            StremioReset.start(this, launch);
            return;
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
