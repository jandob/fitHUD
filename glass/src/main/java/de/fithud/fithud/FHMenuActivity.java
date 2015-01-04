package de.fithud.fithud;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * Created by jandob on 11/18/14.
 */
public class FHMenuActivity extends Activity{
    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        openOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.fithud, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection.
        switch (item.getItemId()) {
            case R.id.stop_this:
                stopService(new Intent(this, FHLiveCardService.class));
                return true;

            case R.id.showMainMenu:
                startActivity(new Intent(FHMenuActivity.this, MainImmersion.class ));
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onOptionsMenuClosed(Menu menu) {
        // Nothing else to do, closing the activity.
        finish();
    }
}
