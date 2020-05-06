package com.softaai.androidassignmentweel

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var circularProgress: CircularProgress
    private lateinit var editProgressTextView: EditText
    private lateinit var animateButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        circularProgress = findViewById(R.id.circular_progress);
        editProgressTextView = findViewById(R.id.edit_progress_view)
        animateButton = findViewById(R.id.animate_button)
        circularProgress.setMaxProgress(100.00);

        circularProgress.setShouldDrawDot(true);
        circularProgress.setDotWidthDp(25)
        circularProgress.dotColor =
            Color.parseColor("#FFA500")
        circularProgress.setFillBackgroundEnabled(false);

        animateButton.setOnClickListener {
            if (editProgressTextView.text.toString().toDouble() <= 0.0) {
                circularProgress.setAnimationEnabled(false);
                circularProgress.setCurrentProgress(0.0)
                editProgressTextView.setText("0")
            } else if (editProgressTextView.text.toString().toDouble() >= 100.0) {
                circularProgress.setAnimationEnabled(true);
                circularProgress.setCurrentProgress(100.0)
                editProgressTextView.setText("100")
            } else {
                circularProgress.setAnimationEnabled(true);
                circularProgress.setCurrentProgress(editProgressTextView.text.toString().toDouble())
            }

        }
    }
}
