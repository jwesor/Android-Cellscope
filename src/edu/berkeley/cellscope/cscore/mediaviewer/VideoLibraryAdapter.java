package edu.berkeley.cellscope.cscore.mediaviewer;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import edu.berkeley.cellscope.cscore.R;

public class VideoLibraryAdapter extends PhotoLibraryAdapter {
	public VideoLibraryAdapter(Activity a, ArrayList<File> fileList,
			ArrayList<String> fileNames, BitmapCache imageCache) {
		super(a, fileList, fileNames, imageCache);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View vi = convertView;
		if(convertView==null)
			vi = inflater.inflate(R.layout.library_list_row, null);

		TextView title = (TextView)vi.findViewById(R.id.path); // title
		ImageView thumbnail =(ImageView)vi.findViewById(R.id.list_image); // thumb image
		title.setText(titles.get(position));
		int size = (int) vi.getResources().getDimension(R.dimen.thumbnail_size);
		ImageLoader.loadVideoThumbnail(thumbnail, files, position, size, cache);
		//thumbnail.setImageBitmap(ImageLoader.loadThumbnailImage(files.get(position).getPath(), width, height));
		//Uri imageUri = Uri.fromFile(files.get(position));
		// thumbnail.setImageURI(imageUri);
		return vi;
	}
}
