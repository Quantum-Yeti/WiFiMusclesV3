package me.theoria.wifimuscles.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * A reactive Wi-Fi signal blob that pulsates and breaks apart when touched.
 */
public class SignalBlobView extends View {

    // Paint for the main blob
    private final Paint blobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Paint for small emitted particles
    private final Paint particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Signal properties
    private int signalLevel = 0; // 0-4
    private int signalColor = Color.GREEN;

    // Phase of pulsation (used for sine wave wobble)
    private float pulsationPhase = 0f;

    // Multiplier for pulsation amplitude (increases on touch)
    private float wobbleMultiplier = 1f;

    // Perimeter nodes representing the blob's shape
    private final List<BlobNode> perimeterNodes = new ArrayList<>();

    // Mini-particles emitted on touch
    private final List<BlobParticle> particles = new ArrayList<>();

    // Animation loop
    private ValueAnimator animation;

    // Random helper for scattering nodes and particles
    private final Random random = new Random();

    // Number of nodes around the blob
    private static final int NODE_COUNT = 12;

    // Blob sizing factors
    private static final float MAX_RADIUS_FACTOR = 0.4f;   // relative to view size
    private static final float WOBBLE_AMPLITUDE_FACTOR = 0.08f;
    private static final float NODE_OFFSET_FACTOR = 0.12f;

    // Scatter strength
    private static final float NODE_SCATTER_DISTANCE = 50f;
    private static final int PARTICLES_PER_TOUCH = 8;

    public SignalBlobView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    private void initialize() {
        // Configure blob paint
        blobPaint.setStyle(Paint.Style.FILL);
        blobPaint.setColor(signalColor);
        blobPaint.setShadowLayer(8f, 0f, 0f, signalColor);

        // Configure particle paint
        particlePaint.setStyle(Paint.Style.FILL);
        particlePaint.setAntiAlias(true);

        // Required for shadow layer to render
        setLayerType(LAYER_TYPE_HARDWARE, null);

        // Initialize perimeter nodes
        for (int i = 0; i < NODE_COUNT; i++) {
            perimeterNodes.add(new BlobNode());
        }

        startAnimationLoop();
    }

    /** Sets the current signal level (0-4) and color */
    public void setSignalLevel(int level, int color) {
        signalLevel = Math.max(0, Math.min(level, 4));
        signalColor = color;
        blobPaint.setColor(signalColor);
        blobPaint.setShadowLayer(20f, 0f, 0f, signalColor);
        invalidate();
    }

    /** Starts the continuous animation loop */
    private void startAnimationLoop() {
        animation = ValueAnimator.ofFloat(0f, 1f);
        animation.setDuration(16); // ~60 FPS
        animation.setRepeatCount(ValueAnimator.INFINITE);
        animation.setInterpolator(null);
        animation.addUpdateListener(anim -> {
            pulsationPhase += 0.02f;
            updateNodePositions();
            updateParticles();
            invalidate();
        });
        animation.start();
    }

    /** Gradually returns nodes to their neutral positions */
    private void updateNodePositions() {
        for (BlobNode node : perimeterNodes) {
            node.offsetX *= 0.85f;
            node.offsetY *= 0.85f;
        }
    }

    /** Updates particle positions and removes faded ones */
    private void updateParticles() {
        Iterator<BlobParticle> iter = particles.iterator();
        while (iter.hasNext()) {
            BlobParticle particle = iter.next();
            particle.x += particle.vx;
            particle.y += particle.vy;
            particle.alpha -= 0.04f; // fade out
            if (particle.alpha <= 0f) iter.remove();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;

        // Maximum radius relative to view size
        float maxRadius = Math.min(getWidth(), getHeight()) * MAX_RADIUS_FACTOR;

        // Base radius depends on signal level
        float baseRadius = (signalLevel / 4f) * maxRadius;

        // Wobbling radius using sine wave
        float wobbleRadius = baseRadius + (float) Math.sin(pulsationPhase * 2f)
                * maxRadius * WOBBLE_AMPLITUDE_FACTOR * wobbleMultiplier;

        // Angle between nodes
        double angleStep = 2 * Math.PI / NODE_COUNT;

        float[] nodeXs = new float[NODE_COUNT];
        float[] nodeYs = new float[NODE_COUNT];

        // Compute node positions
        for (int i = 0; i < NODE_COUNT; i++) {
            double angle = i * angleStep;
            float nodeRadius = wobbleRadius
                    + (float) Math.sin(pulsationPhase * 2 + i) * wobbleRadius * NODE_OFFSET_FACTOR;
            BlobNode node = perimeterNodes.get(i);
            nodeXs[i] = centerX + (float) (nodeRadius * Math.cos(angle)) + node.offsetX;
            nodeYs[i] = centerY + (float) (nodeRadius * Math.sin(angle)) + node.offsetY;
        }

        // Draw the blob path
        Path blobPath = new Path();
        blobPath.moveTo(nodeXs[0], nodeYs[0]);
        for (int i = 0; i < NODE_COUNT; i++) {
            int nextIndex = (i + 1) % NODE_COUNT;
            float controlX = (nodeXs[i] + nodeXs[nextIndex]) / 2;
            float controlY = (nodeYs[i] + nodeYs[nextIndex]) / 2;
            blobPath.quadTo(nodeXs[i], nodeYs[i], controlX, controlY);
        }
        blobPath.close();
        canvas.drawPath(blobPath, blobPaint);

        // Draw particles
        for (BlobParticle particle : particles) {
            particlePaint.setColor(signalColor);
            particlePaint.setAlpha((int) (particle.alpha * 255));
            canvas.drawCircle(particle.x, particle.y, particle.radius, particlePaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                wobbleMultiplier = 1.5f;

                // Scatter perimeter nodes
                for (BlobNode node : perimeterNodes) {
                    float offsetX = (random.nextFloat() - 0.5f) * 2f * NODE_SCATTER_DISTANCE;
                    float offsetY = (random.nextFloat() - 0.5f) * 2f * NODE_SCATTER_DISTANCE;
                    node.offsetX += offsetX;
                    node.offsetY += offsetY;
                }

                // Emit particles from center
                for (int i = 0; i < PARTICLES_PER_TOUCH; i++) {
                    float angle = (float) (random.nextFloat() * 2 * Math.PI);
                    float speed = 4f + random.nextFloat() * 3f;
                    particles.add(new BlobParticle(
                            getWidth() / 2f,
                            getHeight() / 2f,
                            (float) Math.cos(angle) * speed,
                            (float) Math.sin(angle) * speed,
                            6f + random.nextFloat() * 4f
                    ));
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                wobbleMultiplier = 1f;
                break;
        }

        return true;
    }

    /** Represents a perimeter node of the blob */
    private static class BlobNode {
        float offsetX = 0f;
        float offsetY = 0f;
    }

    /** Represents a particle emitted from the blob */
    private static class BlobParticle {
        float x, y;
        float vx, vy;
        float radius;
        float alpha = 1f;

        BlobParticle(float x, float y, float vx, float vy, float radius) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.radius = radius;
        }
    }
}