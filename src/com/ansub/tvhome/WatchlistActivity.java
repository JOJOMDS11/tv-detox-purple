package com.ansub.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
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
import java.util.Map;

public class WatchlistActivity extends Activity {

    private static final String JSON_URL =
            "https://raw.githubusercontent.com/JOJOMDS11/tv-detox-purple/watchlist-data/public/watchlist.json";
    private static final String CACHE_FILE = "watchlist.json";
    private static final long MAX_AGE_MS = 60L * 60L * 1000L;
    private static final String ALL = "Todos";

    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_DIM = 0x99FFFFFF;
    private static final int TEXT_MID = 0xDDFFFFFF;

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
        int year;
        boolean series;
        List<String> genres = new ArrayList<>();
    }

    private final List<Item> all = new ArrayList<>();
    private final List<Item> shown = new ArrayList<>();
    private final List<String> genreNames = new ArrayList<>();
    private final Map<String, Integer> genreCounts = new HashMap<>();
    private String current = ALL;
    private String lastRaw = "";

    private ListView genreList;
    private ListView titleList;
    private TextView headerTitle;
    private TextView headerCount;
    private TextView emptyView;
    private BaseAdapter genreAdapter;
    private BaseAdapter titleAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {0xFF1B0C30, 0xFF0E0719, 0xFF07030F});
        getWindow().setBackgroundDrawable(bg);
        setContentView(buildLayout());
        loadCache();
        refreshIfStale();
    }

    private View buildLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setPadding(dp(56), dp(36), dp(56), dp(28));

        genreAdapter = new GenreAdapter();
        genreList = newList();
        genreList.setAdapter(genreAdapter);
        genreList.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= genreNames.size()) return;
            current = genreNames.get(position);
            applyFilter();
            genreAdapter.notifyDataSetChanged();
            titleList.requestFocus();
        });
        root.addView(genreList, new LinearLayout.LayoutParams(dp(250), ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams rightLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        rightLp.leftMargin = dp(36);
        root.addView(right, rightLp);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.BOTTOM);
        header.setPadding(dp(16), 0, dp(16), dp(14));
        headerTitle = text(26, TEXT, Typeface.BOLD);
        headerCount = text(18, TEXT_DIM, Typeface.NORMAL);
        headerCount.setPadding(dp(14), 0, 0, dp(2));
        header.addView(headerTitle);
        header.addView(headerCount);
        right.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        FrameLayout body = new FrameLayout(this);
        titleAdapter = new TitleAdapter();
        titleList = newList();
        titleList.setAdapter(titleAdapter);
        titleList.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < shown.size()) openInStremio(shown.get(position));
        });
        emptyView = text(20, TEXT_DIM, Typeface.NORMAL);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setText("Carregando…");
        body.addView(titleList, new FrameLayout.LayoutParams(-1, -1));
        body.addView(emptyView, new FrameLayout.LayoutParams(-1, -1));
        titleList.setEmptyView(emptyView);
        right.addView(body, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        updateHeader();
        titleList.requestFocus();
        return root;
    }

    private ListView newList() {
        ListView list = new ListView(this);
        list.setDivider(null);
        list.setDividerHeight(0);
        list.setVerticalScrollBarEnabled(false);
        list.setOverScrollMode(View.OVER_SCROLL_NEVER);
        list.setSelector(rounded(0x33FFFFFF));
        list.setDrawSelectorOnTop(false);
        list.setFocusable(true);
        list.setItemsCanFocus(false);
        list.setClipToPadding(false);
        return list;
    }

    private GradientDrawable rounded(int color) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(dp(10));
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
        if (f.exists() && !all.isEmpty() && System.currentTimeMillis() - f.lastModified() < MAX_AGE_MS) return;
        new Thread(() -> {
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
        }).start();
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
            String type = o.optString("type");
            it.series = type.equals("tvSeries") || type.equals("tvMiniSeries");
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
        genreAdapter.notifyDataSetChanged();
        applyFilter();
    }

    private void applyFilter() {
        shown.clear();
        for (Item it : all) if (current.equals(ALL) || it.genres.contains(current)) shown.add(it);
        titleAdapter.notifyDataSetChanged();
        titleList.setSelection(0);
        updateHeader();
    }

    private void updateHeader() {
        headerTitle.setText(current.equals(ALL) ? "Watchlist" : current);
        headerCount.setText(all.isEmpty() ? "" : String.valueOf(shown.size()));
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

    private final class GenreAdapter extends BaseAdapter {
        @Override public int getCount() { return genreNames.size(); }
        @Override public Object getItem(int position) { return genreNames.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout row;
            TextView name;
            TextView count;
            if (convertView == null) {
                row = new LinearLayout(WatchlistActivity.this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(16), dp(11), dp(16), dp(11));
                name = text(19, TEXT_DIM, Typeface.NORMAL);
                name.setSingleLine(true);
                name.setEllipsize(TextUtils.TruncateAt.END);
                count = text(15, TEXT_DIM, Typeface.NORMAL);
                row.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                row.addView(count, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                row.setTag(new TextView[] {name, count});
            } else {
                row = (LinearLayout) convertView;
            }
            TextView[] views = (TextView[]) row.getTag();
            String g = genreNames.get(position);
            boolean active = g.equals(current);
            views[0].setText(g);
            views[0].setTextColor(active ? TEXT : TEXT_DIM);
            views[0].setTypeface(Typeface.SANS_SERIF, active ? Typeface.BOLD : Typeface.NORMAL);
            Integer n = g.equals(ALL) ? Integer.valueOf(all.size()) : genreCounts.get(g);
            views[1].setText(n == null ? "" : String.valueOf(n));
            return row;
        }
    }

    private final class TitleAdapter extends BaseAdapter {
        @Override public int getCount() { return shown.size(); }
        @Override public Object getItem(int position) { return shown.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout row;
            if (convertView == null) {
                row = new LinearLayout(WatchlistActivity.this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(16), dp(11), dp(16), dp(11));
                TextView title = text(21, TEXT, Typeface.NORMAL);
                title.setSingleLine(true);
                title.setEllipsize(TextUtils.TruncateAt.END);
                TextView meta = text(16, TEXT_DIM, Typeface.NORMAL);
                meta.setSingleLine(true);
                LinearLayout.LayoutParams metaLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                metaLp.leftMargin = dp(20);
                row.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                row.addView(meta, metaLp);
                row.setTag(new TextView[] {title, meta});
            } else {
                row = (LinearLayout) convertView;
            }
            TextView[] views = (TextView[]) row.getTag();
            Item it = shown.get(position);
            views[0].setText(it.title);
            String meta = it.year > 0 ? String.valueOf(it.year) : "";
            if (it.series) meta = meta.isEmpty() ? "Série" : "Série · " + meta;
            views[1].setText(meta);
            return row;
        }
    }
}
