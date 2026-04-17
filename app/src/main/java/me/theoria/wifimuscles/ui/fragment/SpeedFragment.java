package me.theoria.wifimuscles.ui.fragment;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.util.Random;

import me.theoria.wifimuscles.R;
import me.theoria.wifimuscles.ui.helpers.SpeedTestHelper;

public class SpeedFragment extends Fragment {

    @SuppressLint("SetJavaScriptEnabled")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_speed, container, false);

        SwipeRefreshLayout swipeRefresh = view.findViewById(R.id.swipeRefresh);
        WebView webView = view.findViewById(R.id.speedTestWeb);
        //LottieAnimationView animView = view.findViewById(R.id.randAnim);

        AdView adView = view.findViewById(R.id.ad_view);

        // load ad
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                swipeRefresh.setRefreshing(false);
            }

            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                swipeRefresh.setRefreshing(true);
            }
        });

        webView.setBackgroundColor(Color.WHITE);

        webView.loadDataWithBaseURL(
                "https://openspeedtest.com",
                SpeedTestHelper.getEmbedHtml(),
                "text/html",
                "UTF-8",
                null
        );

        // Swipe to refresh with webview
        swipeRefresh.setOnRefreshListener(webView::reload);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {

            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());

            v.setPadding(
                    v.getPaddingLeft(),
                    bars.top + 16,
                    v.getPaddingRight(),
                    bars.bottom + 16
            );

            return insets;
        });
    }

    /*private void setRandomAnimation(LottieAnimationView animation) {

        int[] animations = {
                R.raw.cat_anim,
                R.raw.potato_anim
        };

        int pick = animations[new Random().nextInt(animations.length)];

        if (pick == R.raw.potato_anim) {
            animation.setScaleY(0.6f); // shrink only potato
            animation.setScaleX(0.6f);
        } else {
            animation.setScaleY(1.0f);
        }

        animation.cancelAnimation();
        animation.setAnimation(pick);
        animation.setRepeatCount(LottieDrawable.INFINITE);
        animation.playAnimation();
    }*/
}
