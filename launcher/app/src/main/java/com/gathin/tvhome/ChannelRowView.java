package com.gathin.tvhome;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * ChannelRow: a horizontal row of app tiles (a Projectivy-style "channel").
 * Each row shows apps from one category, scrolls left/right, and the focused tile scales up.
 * Multiple ChannelRow views are stacked vertically to form the channel carousel.
 */
public class ChannelRowView extends HorizontalScrollView {

    private final Context context;
    private final PackageManager pm;
    private String category;
    private final List<ChannelEntry> entries = new ArrayList<>();
    private int selectedPosition = 0;
    private OnChannelTileClickListener clickListener;

    public interface OnChannelTileClickListener {
        void onChannelTileClicked(String packageName, String className, String category);
    }

    public ChannelRowView(Context context) {
        super(context);
        this.context = context;
        this.pm = context.getPackageManager();
        this.category = null;
        init();
    }

    public ChannelRowView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        this.pm = context.getPackageManager();
        this.category = null;
        init();
    }

    public ChannelRowView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        this.pm = context.getPackageManager();
        this.category = null;
        init();
    }

    private void init() {
        setHorizontalScrollBarEnabled(false);
        setOverScrollMode(OVER_SCROLL_NEVER);
    }

    public void setCategory(String category, List<String> packageNames) {
        this.category = category;
        entries.clear();
        for (String pkg : packageNames) {
            try {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.setPackage(pkg);
                List<ResolveInfo> ris = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                if (!ris.isEmpty()) {
                    ResolveInfo ri = ris.get(0);
                    entries.add(new ChannelEntry(
                            pkg,
                            ri.activityInfo.name,
                            ri.activityInfo.loadIcon(pm),
                            ri.activityInfo.loadLabel(pm).toString()));
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

        int sizeDp = 100;
        int spacingDp = 14;
        float density = context.getResources().getDisplayMetrics().density;
        int sizePx = (int) (sizeDp * density);
        int spacingPx = (int) (spacingDp * density);

        for (int i = 0; i < entries.size(); i++) {
            ChannelEntry entry = entries.get(i);
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

    private View makeTile(ChannelEntry entry, boolean selected, int sizePx) {
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
                (int) (sizePx * 1.4f), ViewGroup.LayoutParams.WRAP_CONTENT);
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
                clickListener.onChannelTileClicked(entry.packageName, entry.className, category);
            }
        });
        tile.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                selectedPosition = entries.indexOf(entry);
                rebuild();
                post(() -> smoothScrollTo(v.getLeft() - (getWidth() / 2) + (v.getWidth() / 2), 0));
            }
        });

        return tile;
    }

    public void setOnChannelTileClickListener(OnChannelTileClickListener listener) {
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

    private static class ChannelEntry {
        final String packageName;
        final String className;
        final Drawable icon;
        final String label;

        ChannelEntry(String packageName, String className, Drawable icon, String label) {
            this.packageName = packageName;
            this.className = className;
            this.icon = icon;
            this.label = label;
        }
    }
}
