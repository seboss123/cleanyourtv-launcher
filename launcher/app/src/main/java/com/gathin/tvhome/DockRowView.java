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

import androidx.annotation.Nullable;

import java.util.List;

/**
 * DockRow: a horizontally scrollable row of dock apps, like Projectivy's channel row but for dock apps.
 * Shows more than COLUMNS dock apps — swipe left/right to see them all. The focused tile scales up.
 */
public class DockRowView extends HorizontalScrollView {

    private final Context context;
    private final PackageManager pm;
    private List<String> packageNames;
    private int selectedPosition = 0;
    private OnDockTileClickListener clickListener;

    public interface OnDockTileClickListener {
        void onDockTileClicked(String packageName, String className);
    }

    public DockRowView(Context context) {
        super(context);
        this.context = context;
        this.pm = context.getPackageManager();
        this.packageNames = null;
        init();
    }

    public DockRowView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        this.pm = context.getPackageManager();
        this.packageNames = null;
        init();
    }

    public DockRowView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        this.pm = context.getPackageManager();
        this.packageNames = null;
        init();
    }

    private void init() {
        setHorizontalScrollBarEnabled(false);
        setOverScrollMode(OVER_SCROLL_NEVER);
    }

    public void setDockApps(List<String> packageNames) {
        this.packageNames = packageNames;
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

        for (int i = 0; i < packageNames.size(); i++) {
            String pkg = packageNames.get(i);
            try {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.setPackage(pkg);
                List<ResolveInfo> ris = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
                if (!ris.isEmpty()) {
                    ResolveInfo ri = ris.get(0);
                    View tile = makeTile(ri.activityInfo.packageName, ri.activityInfo.name,
                            ri.activityInfo.loadIcon(pm), ri.activityInfo.loadLabel(pm).toString(),
                            i == selectedPosition, sizePx);
                    container.addView(tile);
                    if (i < packageNames.size() - 1) {
                        View spacer = new View(context);
                        spacer.setLayoutParams(new LayoutParams(spacingPx, ViewGroup.LayoutParams.MATCH_PARENT));
                        container.addView(spacer);
                    }
                }
            } catch (Exception e) {
                // Skip packages that can't be resolved
            }
        }

        addView(container);
    }

    private View makeTile(String pkg, String cls, Drawable icon, String label, boolean selected, int sizePx) {
        LinearLayout tile = new LinearLayout(context);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        tile.setPadding(8, 4, 8, 4);

        ImageView iconView = new ImageView(context);
        iconView.setImageDrawable(icon);
        int iconSize = (int) (sizePx * 0.7f);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
        tile.addView(iconView, iconParams);

        TextView labelView = new TextView(context);
        labelView.setText(label);
        labelView.setTextSize(12f);
        labelView.setTextColor(selected ? 0xFFFFFFFF : 0xB3FFFFFF);
        labelView.setSingleLine(true);
        labelView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        labelView.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                (int) (sizePx * 1.4f), ViewGroup.LayoutParams.WRAP_CONTENT);
        tile.addView(labelView, labelParams);

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
                clickListener.onDockTileClicked(pkg, cls);
            }
        });
        tile.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                selectedPosition = packageNames.indexOf(pkg);
                rebuild();
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
        return packageNames == null ? 0 : packageNames.size();
    }
}
