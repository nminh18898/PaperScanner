package com.hcmus.thesis.nhatminhanhkiet.documentscanner.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.hcmus.thesis.nhatminhanhkiet.documentscanner.R;

import java.util.ArrayList;

public class ImageListAdapter extends RecyclerView.Adapter<ImageListAdapter.ImageListHolder> {
    ArrayList<ImageInfo> imageList;
    Context context;

    public ImageListAdapter(Context context, ArrayList<ImageInfo> imageList) {
        this.context = context;
        this.imageList = imageList;
    }


    @NonNull
    @Override
    public ImageListHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.image_list_item, parent, false);
        return new ImageListHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageListHolder holder, int position) {
        ImageInfo imageInfo = imageList.get(position);
        holder.bind(imageInfo);
    }



    @Override
    public int getItemCount() {
        return imageList.size();
    }

    public class ImageListHolder extends RecyclerView.ViewHolder{
        ImageView imageView;
        TextView tvName, tvDimension, tvSize, tvDateCreated;

        public ImageListHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.ivImage);
            tvName = itemView.findViewById(R.id.tvName);
            tvDateCreated = itemView.findViewById(R.id.tvDateCreated);
            tvDimension = itemView.findViewById(R.id.tvDimension);
            tvSize = itemView.findViewById(R.id.tvSize);
        }

        public void bind(ImageInfo imageInfo){
            Glide.with(context).load(imageInfo.filePath)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .skipMemoryCache(true)
                    .into(imageView);

            tvName.setText(imageInfo.fileName);
            tvDimension.setText("Dimension: " + imageInfo.imageWidth + "x" + imageInfo.imageHeight);
            tvDateCreated.setText("Date created: " + imageInfo.dateCreated);
            tvSize.setText("Size: " + imageInfo.fileSize);
        }
    }
}
