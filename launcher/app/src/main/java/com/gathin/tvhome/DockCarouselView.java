package com.gathin.tvhome;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * DockCarousel: a horizontally scrollable row of dock tiles, like Projectivy's channel row but for apps.
 * Shows more than COLUMNS dock apps — swipe left/right to see them all. The focused tile scales up.
 */
public class DockCarouselView extends HorizontalScrollView {

    private final Context context;
    private final PackageManager pm;
    private final List<DockEntry> entries = new ArrayList<>();
    private int selectedPosition = 0;
    private OnDockTileClickListener clickListener;

    public interface OnDockTileClickListener {
        void onDockTileClicked(String packageName, String className);
    }

    public DockCarouselView(Context context) {
        super(context);
        this.context = context;
        this.pm = context.getPackageManager();
        init();
    }

    public DockCarouselView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        this.pm = context.getPackageManager();
        init();
    }

    public DockCarouselView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        this.pm = context.getPackageManager();
        init();
    }

    private void init() {
        setHorizontalScrollBarEnabled(false);
        setOverScrollMode(OVER_SCROLL_NEVER);
    }

    public void setDockApps(List<String> packageNames) {
        entries.clear();
        for (String pkg : packageNames) {
            try {
                android.content.ComponentName cn = new android.content.ComponentName(pkg, pkg + ".MainActivity");
                // Try to resolve the launch intent
                android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_MAIN);
                intent.addCategory(android.content.Intent.CATEGORY_HOME);
                List<ResolveInfo> ris = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo ri : ris) {
                    if (ri.activityInfo.packageName.equals(pkg)) {
                        entries.add(new DockEntry(pkg, ri.activityInfo.name,
                                ri.activityInfo.loadIcon(pm), ri.activityInfo.loadLabel(pm).toString()));
                        break;
                    }
                }
                if (entries.isEmpty() || entries.get(entries.size() - 1).packageName == null) {
                    // Fallback: create entry without resolved info
                    Drawable icon = pm.getApplicationIcon(pkg);
                    String label = pkg;
                    try {
                        label = (String) pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0));
                    } catch (Exception e) { /* keep pkg */ }
                    entries.add(new DockEntry(pkg, pkg + ".MainActivity", icon, label));
                }
            } catch (Exception e) {
                // Skip packages that can't be resolved
            }
        }
        rebuild();
    }

    private void rebuild() {
        removeAllViews();
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        int sizeDp = 110;
        int spacingDp = 16;
        float density = context.getResources().getDisplayMetrics().density;
        int sizePx = (int) (sizeDp * density);
        int spacingPx = (int) (spacingDp * density);

        for (int i = 0; i < entries.size(); i++) {
            DockEntry entry = entries.get(i);
            View tile = makeTile(entry, i == selectedPosition, sizePx);
            container.addView(tile);
            if (i < entries.size() - 1) {
                View spacer = new View(context);
                spacer.setLayoutParams(new LayoutParams(spacingPx, ViewGroup.LayoutParams.MATCH_PARENT));
                container.addView(spacer);
            }
        }

        addView(container);
    }

    private View makeTile(DockEntry entry, boolean selected, int sizePx) {
        LinearLayout tile = new LinearLayout(context);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        tile.setPadding(8, 4, 8, 4);

        ImageView icon = new ImageView(context);
        icon.setImageDrawable(entry.icon);
        int iconSize = (int) (sizePx * 0.7f);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
        tile.addView(icon, iconParams);

        TextView label = new TextView(context);
        label.setText(entry.label);
        label.setTextSize(12f);
        label.setTextColor(selected ? 0xFFFFFFFF : 0xB3FFFFFF);
        label.setSingleLine(true);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        label.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                (int) (sizePx * 1.5f), ViewGroup.LayoutParams.WRAP_CONTENT);
        tile.addView(label, labelParams);

        if (selected) {
            tile.setBackgroundResource(R.drawable.bg_category_chip_selected);
            tile.setScaleX(1.1f);
            tile.setScaleY(1.1f);
        } else {
            tile.setBackgroundResource(R.drawable.bg_category_chip);
        }

        tile.setFocusable(true);
        tile.setFocusableInTouchMode(true);
        tile.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onDockTileClicked(entry.packageName, entry.className);
            }
        });
        tile.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                selectedPosition = entries.indexOf(entry);
                rebuild();
                // Scroll selected tile into center
                post(() -> smoothScrollTo(v.getLeft() - (getWidth() / 2) + (v.getWidth() / 2), 0));
            }
        });

        return tile;
    }

    public void setOnDockTileClickListener(OnDockTileClickListener listener) {
        this.clickListener = listener;
    }

    public void scrollLeft() {
        smoothScrollBy(-getWidth() / 3, 0);
    }

    public void scrollRight() {
        smoothScrollBy(getWidth() / 3, 0);
    }

    public int getItemCount() {
        return entries.size();
    }

    private static class DockEntry {
        final String packageName;
        final String className;
        final Drawable icon;
        final String label;

        DockEntry(String packageName, String className, Drawable icon, String label) {
            this.packageName = packageName;
            this.className = className;
            this.icon = icon;
            this.label = label;
        }
    }
}
