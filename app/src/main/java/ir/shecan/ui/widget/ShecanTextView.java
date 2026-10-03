package ir.shecan.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

public class ShecanTextView extends AppCompatTextView {


    public ShecanTextView(@NonNull Context context) {
        super(context);
        run(context);
    }

    public ShecanTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        run(context);
    }

    public ShecanTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        run(context);
    }

    private void run(Context context) {
        try {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, getTextSize());
        } catch (Exception e) {

        }

//        Log.d("ShecanTextView", "run: "+ convertPixelsToDp(getTextSize() , context));
    }

//    public float convertPixelsToDp(float px, Context context){
//        return px / ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
//    }

//    @Override
//    public void setTextSize(int unit, float size) {
//        try {
//            super.setTextSize(unit, size * AppController.getInstance().userData.getTextSize());
//        } catch (Exception e) {
//
//        }
//
//    }
}

