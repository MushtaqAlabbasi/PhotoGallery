package com.mushtaq.photogallery;


import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class PhotoGalleryFragment extends Fragment {

    private RecyclerView mPhotoRecyclerView;
    private static final String TAG = "PhotoGalleryFragment";
    private List<GalleryItem> mItems = new ArrayList<>();
    private GridLayoutManager mLayoutManager;
    private PhotoAdapter mPhotoAdapter;
    private int mCurrentPage;
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;

    public Button btn1;


    public PhotoGalleryFragment() {
        // Required empty public constructor
    }


    public static Fragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mCurrentPage = 1;
        new FetchItemsTask().execute(mCurrentPage++);

        Handler responseHandler = new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);



        mThumbnailDownloader.setThumbnailDownloadListener(
                new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
                    @Override
                    public void onThumbnailDownloaded(PhotoHolder photoHolder, Bitmap bitmap) {


                        Drawable drawable = new BitmapDrawable(getResources(), bitmap);
                        photoHolder.bindDrawable(drawable);
                    }
                }

        );


        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();

        Log.i(TAG, "Background thread started");
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);
        mPhotoRecyclerView = (RecyclerView) v.findViewById(R.id.photo_recycler_view);
        mLayoutManager = new GridLayoutManager(getActivity(), 3);
        mPhotoRecyclerView.setLayoutManager(mLayoutManager);


//        btn1=v.findViewById(R.id.btn1);

        //Challenge: Paging
        mPhotoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if(!recyclerView.canScrollVertically(1)) {
                    new FetchItemsTask().execute(mCurrentPage++);
                }
            }
        });



        //Dynamically Adjusting the Number of Columns
        mPhotoRecyclerView.getViewTreeObserver()
                .addOnGlobalLayoutListener(
                        new ViewTreeObserver
                                .OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                int numberOfColumns = 1;
                                int width = mPhotoRecyclerView.getWidth();
                                int widthOfSingleElement = 240;
                                if (width > widthOfSingleElement) {
                                    numberOfColumns = width / widthOfSingleElement;
                                }
                                mLayoutManager.setSpanCount(numberOfColumns);
                              //  Log.i(TAG, "numberOfColumns:" + numberOfColumns);
                            }
                        }
                );

        setupAdapter();

//
//        Thread thread1=new Thread(new Runnable() {
//            @Override
//            public void run() {
//                //test1("thread1");
//
//                btn1.setText("hamooood");
//
//            }
//        });


//        Thread thread2=new Thread(new Runnable() {
//            @Override
//            public void run() {
//                test1("thread2");
//            }
//        });
//
//
  //      thread1.start();
//        thread2.start();

        return v;
    }

// --------- end of onCreateView ----------------

    private void updateAdapter() {
        if (mPhotoAdapter !=null){
            mPhotoRecyclerView.getAdapter().notifyDataSetChanged();
        }
    }

    private void setupAdapter() {
        if (isAdded()) {
            mPhotoAdapter = new PhotoAdapter(mItems);
            mPhotoRecyclerView.setAdapter(mPhotoAdapter);
        }
    }



//    public int num1=0;
//
//   public synchronized   int test1(String s){
//
//        for (int i =0 ;i<10;i++){
//            num1+=1;
//            Log.d(TAG,num1 + "  " + s);
//        }
//
//        return num1;
//    }

//---------------------photo Adapter  and View holder -------------------//

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {
        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View view = inflater.inflate(R.layout.list_item_gallery, viewGroup, false);
            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(PhotoHolder photoHolder, int position) {
            GalleryItem galleryItem = mGalleryItems.get(position);
            Drawable placeholder = getResources().getDrawable(R.drawable.ic_image);
            photoHolder.bindDrawable(placeholder);
            mThumbnailDownloader.queueThumbnail(photoHolder, galleryItem.getUrl());
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }


    private class PhotoHolder extends RecyclerView.ViewHolder {

        private ImageView mItemImageView;

        public PhotoHolder(View itemView) {
            super(itemView);
            mItemImageView = (ImageView) itemView.findViewById(R.id.item_image_view);

        }

        public void bindDrawable(Drawable drawable) {
            mItemImageView.setImageDrawable(drawable);
        }
    }


//-----------------------------------------------------------
    private class FetchItemsTask extends AsyncTask<Integer, Void, List<GalleryItem>> {


        @Override
        protected List<GalleryItem> doInBackground(Integer... pageNumber) {

            return new FlickrFetchr().fetchItems(pageNumber[0]);
        }


        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            mItems.addAll(items);
            updateAdapter();
        }
    }
//-----------------------------------------------


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
        Log.i(TAG, "Background thread destroyed");
    }


}
