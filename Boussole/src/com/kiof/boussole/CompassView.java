package com.kiof.boussole;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

public class CompassView extends View {
    private float northOrientation = 0;

//    private Paint circlePaint;
    private Paint northPaint, southPaint, northShadowPaint, southShadowPaint;
    
    private Path trianglePath, shadowPath;

    //Delais entre chaque image
    private final int DELAY = 20;
    //Duree de l'animation
    private final int DURATION = 500;
    private final int WIDTH = 14;
    private final int LENGTH = 25;
    
    private float startNorthOrientation, endNorthOrientation;

    //Heure de debut de l'animation (ms)
    private long startTime;
    
    //Pourcentage d'evolution de l'animation
    private float perCent;
    //Temps courant
    private long curTime;
    //Temps total depuis le debut de l'animation
    private long totalTime;
    
    private Runnable animationTask = new Runnable() {
        public void run() {
            curTime   = SystemClock.uptimeMillis();
            totalTime = curTime - startTime;

            if (totalTime > DURATION) {
                northOrientation = endNorthOrientation % 360;
                removeCallbacks(animationTask);
            } else {
                perCent = ((float) totalTime) / DURATION;

                // Animation plus realiste de l'aiguille
                perCent          = (float) Math.sin(perCent * 1.5);
                perCent          = Math.min(perCent, 1);
                northOrientation = (float) (startNorthOrientation + perCent * (endNorthOrientation - startNorthOrientation));
                postDelayed(this, DELAY);
            }

            // on demande notre vue de se redessiner
            invalidate();
        }
    };

    // Constructeur par defaut de la vue
    public CompassView(Context context) {
        super(context);
        initView();
    }

	// Constructeur utilise pour instancier dans un fichier XML
	public CompassView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView();
	}

	// idem au precedant
	public CompassView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initView();
	}

	// permet de recuperer l'orientation de la boussole
	public float getNorthOrientation() {
		return northOrientation;
	}

	// permet de changer l'orientation de la boussole
	public void setNorthOrientation(float rotation) {

        // on met a jour l'orientation uniquement si elle a change
        if (rotation != this.northOrientation) {
            //Arreter l'ancienne animation
            removeCallbacks(animationTask);
            
        	//Position courante
            this.startNorthOrientation = this.northOrientation;
            //Position desiree
            this.endNorthOrientation   = rotation;

			// Determination du sens de rotation de l'aiguille
			if (((startNorthOrientation + 180) % 360) > endNorthOrientation) {
				// Rotation vers la gauche
				if ((startNorthOrientation - endNorthOrientation) > 180) {
					endNorthOrientation += 360;
				}
			} else {
				// Rotation vers la droite
				if ((endNorthOrientation - startNorthOrientation) > 180) {
					startNorthOrientation += 360;
				}
			}
			
			// Nouvelle animation
			startTime = SystemClock.uptimeMillis();
			postDelayed(animationTask, DELAY);
		}
	}

    // Initialisation de la vue
    private void initView() {
        Resources r = this.getResources();

        // Paint pour l'arriere plan de la boussole
//        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
//        circlePaint.setColor(r.getColor(R.color.compassCircle));

        // Paint pour les 2 aiguilles, Nord et Sud
        northPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        northPaint.setColor(r.getColor(R.color.northPointer));
        northShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        northShadowPaint.setColor(r.getColor(R.color.northShadow));
        southPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        southPaint.setColor(r.getColor(R.color.southPointer));
        southShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        southShadowPaint.setColor(r.getColor(R.color.southShadow));

        // Path pour dessiner les aiguilles
        trianglePath = new Path();
        shadowPath = new Path();
    }

	// Definir la taille de la vue (defaut 200x200 sinon redefini)
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// Notre vue sera un carre, on garde donc le minimum
		int d = Math.min(measure(widthMeasureSpec), measure(heightMeasureSpec));
		setMeasuredDimension(d, d);
	}

	// Determiner la taille de notre vue
	private int measure(int measureSpec) {
		if (MeasureSpec.getMode(measureSpec) == MeasureSpec.UNSPECIFIED) {
			// Taille par defaut
			return 200;
		} else {
			// On va prendre la taille de la vue parente
			return MeasureSpec.getSize(measureSpec);
		}
	}

    @Override
    protected void onDraw(Canvas canvas) {
        int centerX = getMeasuredWidth() / 2;
        int centerY = getMeasuredHeight() / 2;
        int length = Math.round(getMeasuredWidth() / 2 /100 * LENGTH);

        // On determine le diametre du cercle (arriere plan de la boussole)
//        int radius = Math.min(centerX, centerY);
//        canvas.drawCircle(centerX, centerY, radius, circlePaint);

        // On sauvegarde la position initiale du canvas
        canvas.save();

        // On tourne le canvas pour que le nord pointe vers le haut
        canvas.rotate(-northOrientation, centerX, centerY);

        // on creer une forme triangulaire qui part du centre du cercle et pointe vers le haut
        trianglePath.reset();    // RAZ du path (une seule instance)
        trianglePath.moveTo(centerX, length);
        trianglePath.lineTo(centerX - WIDTH, centerY);
        trianglePath.lineTo(centerX, centerY);

        shadowPath.reset();    // RAZ du path (une seule instance)
        shadowPath.moveTo(centerX, length);
        shadowPath.lineTo(centerX + WIDTH, centerY);
        shadowPath.lineTo(centerX, centerY);

        
        // On designe l'aiguille Nord
        canvas.drawPath(trianglePath, northPaint);
        canvas.drawPath(shadowPath, northShadowPaint);

        // On tourne notre vue de 180 pour designer l'auguille Sud
        canvas.rotate(180, centerX, centerY);
        canvas.drawPath(trianglePath, southPaint);
        canvas.drawPath(shadowPath, southShadowPaint);

        // On restaure la position initiale (inutile, mais prevoyant)
        canvas.restore();
    }
}
