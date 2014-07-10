package cw.kop.autowallpaper.websites;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.HashMap;

import cw.kop.autowallpaper.R;
import cw.kop.autowallpaper.settings.AppSettings;

public class WebsiteListFragment extends ListFragment {

	private WebsiteListAdapter listAdapter;
    private Context context;
	
	public WebsiteListFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
//		setHasOptionsMenu(true);
		
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		
		inflater.inflate(R.menu.website_actions, menu);
		
		super.onCreateOptionsMenu(menu, inflater);
	}

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = getActivity();
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.add_website:
				showDialogForInput();
				return true;
			default:
			    return super.onOptionsItemSelected(item);
		}
	}

	private void showDialogForInput() {

        int themeId;

        if(AppSettings.getTheme() == R.style.AppLightTheme) {
            themeId = R.style.LightDialogTheme;
        }
        else {
            themeId = R.style.DarkDialogTheme;
        }

		AlertDialog.Builder dialog = new AlertDialog.Builder(context, themeId);
		dialog.setMessage("Enter website");

        View dialogView = View.inflate(new ContextThemeWrapper(context, themeId), R.layout.add_website_dialog, null);
		
		dialog.setView(dialogView);
		
		final EditText websiteTitle = (EditText) dialogView.findViewById(R.id.website_title);
		final EditText websiteUrl = (EditText) dialogView.findViewById(R.id.website_url);
		final EditText numImages = (EditText) dialogView.findViewById(R.id.num_images);
		
        dialog.setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int id) {
	        	if (!websiteUrl.getText().toString().equals("") && !websiteTitle.getText().toString().equals("")){
	        		
	        		if (!websiteUrl.getText().toString().contains("http")) {
	        			websiteUrl.setText("http://" + websiteUrl.getText().toString());
	        		}
	        		
	        		if (numImages.getText().toString().equals("")) {
	        			numImages.setText("1");
	        		}
	        		
	        		listAdapter.addItem(websiteTitle.getText().toString(), websiteUrl.getText().toString(), true, numImages.getText().toString());
	        	}
	        }
        });
        dialog.setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int id) {
		    }
        });
	    dialog.show();
	}
	
	private void showDialogForChange(final int position) {
		
		final HashMap<String, String> clickedItem = listAdapter.getItem(position);
		
		AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
		dialog.setMessage("Enter website");
		
		LayoutInflater inflater = getActivity().getLayoutInflater();
		
		View dialogView = inflater.inflate(R.layout.add_website_dialog, null);
		
		dialog.setView(dialogView);
		
		final EditText websiteTitle = (EditText) dialogView.findViewById(R.id.website_title);
		final EditText websiteUrl = (EditText) dialogView.findViewById(R.id.website_url);
		final EditText numImages = (EditText) dialogView.findViewById(R.id.num_images);
		
		websiteTitle.setText(clickedItem.get("title"));
		websiteUrl.setText(clickedItem.get("url"));
		numImages.setText(clickedItem.get("num"));
		
        dialog.setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int id) {
	        	if (!websiteUrl.getText().toString().equals("") && !websiteTitle.getText().toString().equals("")){
	        		
	        		if (!websiteUrl.getText().toString().contains("http")) {
	        			websiteUrl.setText("http://" + websiteUrl.getText().toString());
	        		}
	        		
	        		if (numImages.getText().toString().equals("")) {
	        			numImages.setText("1");
	        		}
	        		
	        		listAdapter.setItem(position, websiteTitle.getText().toString(), websiteUrl.getText().toString(), Boolean.valueOf(clickedItem.get("use")), numImages.getText().toString());
	        	}
	        }
        });
        dialog.setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int id) {
		    }
        });
	    dialog.show();
	}
	
	private void showDialogMenu(final int position) {
		AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
		
		dialog.setItems(R.array.website_entry_menu, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
					case 0: 
						showDialogForChange(position);
						break;
					case 1:	
						listAdapter.removeItem(position);
					default:
				}
				
			}
		});
		
		dialog.show();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		return super.onCreateView(inflater, container, savedInstanceState); 
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onActivityCreated(savedInstanceState);
		
		if (listAdapter == null) {
			listAdapter = new WebsiteListAdapter(getActivity());
			for (int i = 0; i < AppSettings.getNumWebsites(); i++) {
				listAdapter.addItem(AppSettings.getWebsiteTitle(i), AppSettings.getWebsiteUrl(i), AppSettings.useWebsite(i), "" + AppSettings.getNumImages(i));
			}
		}
		setListAdapter(listAdapter);
        
		TextView emptyText = new TextView(getActivity());
		emptyText.setText("List is empty. Please add new website entry.");
		emptyText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
		emptyText.setGravity(Gravity.CENTER_HORIZONTAL);

		LinearLayout emptyLayout = new LinearLayout(getActivity());
		emptyLayout.setOrientation(LinearLayout.VERTICAL);
		emptyLayout.setGravity(Gravity.TOP);
		emptyLayout.addView(emptyText);
		
		((ViewGroup) getListView().getParent()).addView(emptyLayout, 0);
		
		getListView().setEmptyView(emptyLayout);
		
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		showDialogMenu(position);
		
	}
	
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	@Override
	public void onDestroyView() {
		// TODO Auto-generated method stub
		super.onDestroyView();
		setListAdapter(null);
	}

	@Override
	public void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		listAdapter.saveData();
		
	}

	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}

	@Override
	public void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
	}

	@Override
	public void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
	}

}
