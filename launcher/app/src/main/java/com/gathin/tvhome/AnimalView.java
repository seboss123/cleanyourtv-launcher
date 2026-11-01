package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * AnimalView: A container that spawns animated pixel-art animals that roam
 * around the home screen. Animal type and count are driven by weather + time.
 * Inspired by lofi aesthetic — animals are calm, decorative, and reactive.
 */
public class AnimalView extends RelativeLayout {

    private static final int MAX_ANIMALS = 3;
    private static final long ROAM_INTERVAL_MS = 4000; // how often each animal moves
    private static final int ANIMAL_SIZE_DP = 120;

    private final Context context;
    private final Random random = new Random();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<AnimalEntry> animals = new ArrayList<>();
    private final Runnable roamRunnable = this::roamAnimals;

    private String currentWeather = null;
    private boolean isNight = false;
    private int screenW = 0, screenH = 0;

    public AnimalView(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public AnimalView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    public AnimalView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        init();
    }

    private void init() {
        setWillNotDraw(false);
        loadPrefs();
    }

    private void loadPrefs() {
        SharedPreferences prefs = context.getSharedPreferences("tvhome_prefs", Context.MODE_PRIVATE);
        currentWeather = prefs.getString("weather_condition", null);
        isNight = prefs.getBoolean("is_night", false);
    }

    public void setWeather(String weather, boolean night) {
        this.currentWeather = weather;
        this.isNight = night;
        rebuildAnimals();
    }

    private void rebuildAnimals() {
        removeAllViews();
        animals.clear();

        int count = animalCountForWeather();
        List<String> types = animalTypesForWeather();

        for (int i = 0; i < count && i < types.size(); i++) {
            addAnimal(types.get(i));
        }

        startRoaming();
    }

    private int animalCountForWeather() {
        if (currentWeather == null) return 1;
        switch (currentWeather) {
            case WeatherFetcher.CONDITION_CLEAR:
                return isNight ? 1 : 2; // fewer cats at night (they're sleeping)
            case WeatherFetcher.CONDITION_RAIN:
            case WeatherFetcher.CONDITION_DRIZZLE:
            case WeatherFetcher.CONDITION_THUNDERSTORM:
                return 1; // one fish hiding
            case WeatherFetcher.CONDITION_SNOW:
                return 2; // rabbits in the snow
            case WeatherFetcher.CONDITION_CLOUDS:
            case WeatherFetcher.CONDITION_FOG:
                return 2; // dogs wandering
            default:
                return 1;
        }
    }

    private List<String> animalTypesForWeather() {
        List<String> types = new ArrayList<>();
        if (currentWeather == null) {
            types.add("cat");
            return types;
        }
        switch (currentWeather) {
            case WeatherFetcher.CONDITION_CLEAR:
                types.add("cat");
                if (!isNight) types.add("bird");
                break;
            case WeatherFetcher.CONDITION_RAIN:
            case WeatherFetcher.CONDITION_DRIZZLE:
            case WeatherFetcher.CONDITION_THUNDERSTORM:
                types.add("fish");
                break;
            case WeatherFetcher.CONDITION_SNOW:
                types.add("rabbit");
                types.add("rabbit");
                break;
            case WeatherFetcher.CONDITION_CLOUDS:
            case WeatherFetcher.CONDITION_FOG:
                types.add("dog");
                types.add("dog");
                break;
            default:
                types.add("cat");
        }
        return types;
    }

    private void addAnimal(String type) {
        ImageView animalView = new ImageView(context);
        int resId = getAnimResId(type);
        animalView.setImageResource(resId);

        AnimationDrawable anim = (AnimationDrawable) animalView.getDrawable();
        if (anim != null) {
            anim.start();
        }

        int sizeDp = ANIMAL_SIZE_DP + random.nextInt(40); // 120-160dp
        int sizePx = dpToPx(sizeDp);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(sizePx, sizePx);
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        animalView.setLayoutParams(params);

        animalView.setAlpha(0.85f);
        animalView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);

        addView(animalView);

        animals.add(new AnimalEntry(animalView, type, randomNextPosition()));
        moveAnimalTo(animals.size() - 1);
    }

    private int getAnimResId(String type) {
        switch (type) {
            case "cat": return R.drawable.anim_cat;
            case "dog": return R.drawable.anim_dog;
            case "bird": return R.drawable.anim_bird;
            case "fish": return R.drawable.anim_fish;
            case "rabbit": return R.drawable.anim_rabbit;
            default: return R.drawable.anim_cat;
        }
    }

    private void startRoaming() {
        handler.removeCallbacks(roamRunnable);
        handler.postDelayed(roamRunnable, ROAM_INTERVAL_MS);
    }

    private void roamAnimals() {
        for (int i = 0; i < animals.size(); i++) {
            moveAnimalTo(i);
        }
        handler.postDelayed(roamRunnable, ROAM_INTERVAL_MS);
    }

    private void moveAnimalTo(int index) {
        if (index >= animals.size()) return;
        AnimalEntry entry = animals.get(index);
        ImageView view = entry.view;

        int[] pos = randomNextPosition();
        entry.lastX = pos[0];
        entry.lastY = pos[1];

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) view.getLayoutParams();
        params.leftMargin = pos[0];
        params.topMargin = pos[1];
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
        view.setLayoutParams(params);

        // Slight scale variation for personality
        float scale = 0.85f + random.nextFloat() * 0.3f;
        view.setScaleX(scale);
        view.setScaleY(scale);

        // Flip horizontally sometimes (animals face different directions)
        if (random.nextBoolean()) {
            view.setScaleX(-Math.abs(view.getScaleX()));
        }
    }

    private int[] randomNextPosition() {
        int maxX = Math.max(0, screenW - dpToPx(ANIMAL_SIZE_DP + 40));
        int maxY = Math.max(0, screenH - dpToPx(ANIMAL_SIZE_DP + 40));
        return new int[] {
            random.nextInt(maxX),
            random.nextInt(maxY)
        };
    }

    private int dpToPx(int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenW = w;
        screenH = h;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeCallbacks(roamRunnable);
        for (AnimalEntry entry : animals) {
            Drawable d = entry.view.getDrawable();
            if (d instanceof AnimationDrawable) {
                ((AnimationDrawable) d).stop();
            }
        }
        animals.clear();
    }

    private static class AnimalEntry {
        ImageView view;
        String type;
        int lastX, lastY;

        AnimalEntry(ImageView view, String type, int[] pos) {
            this.view = view;
            this.type = type;
            this.lastX = pos[0];
            this.lastY = pos[1];
        }
    }
}
