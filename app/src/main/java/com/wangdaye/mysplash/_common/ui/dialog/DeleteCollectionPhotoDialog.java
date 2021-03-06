package com.wangdaye.mysplash._common.ui.dialog;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.github.rahatarmanahmed.cpv.CircularProgressView;
import com.wangdaye.mysplash.Mysplash;
import com.wangdaye.mysplash.R;
import com.wangdaye.mysplash._common.data.entity.unsplash.ChangeCollectionPhotoResult;
import com.wangdaye.mysplash._common.data.entity.unsplash.Collection;
import com.wangdaye.mysplash._common.data.entity.unsplash.Photo;
import com.wangdaye.mysplash._common.data.service.CollectionService;
import com.wangdaye.mysplash._common.utils.AnimUtils;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Delete collection photo dialog.
 * */

public class DeleteCollectionPhotoDialog extends DialogFragment
        implements View.OnClickListener, CollectionService.OnChangeCollectionPhotoListener {
    // widget
    private RelativeLayout confirmContainer;
    private CircularProgressView progressView;

    private OnDeleteCollectionListener listener;

    // data
    private CollectionService service;

    private Collection collection;
    private Photo photo;
    private int position;

    private int state;
    private static final int CONFIRM_STATE = 0;
    private static final int DELETE_STATE = 1;

    /** <br> life cycle. */

    @SuppressLint("InflateParams")
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Mysplash.getInstance().setActivityInBackstage(true);
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_delete_collection_photo, null, false);
        initData();
        initWidget(view);
        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .create();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Mysplash.getInstance().setActivityInBackstage(false);
        service.cancel();
    }

    /** <br> UI. */

    private void initWidget(View v) {
        this.confirmContainer = (RelativeLayout) v.findViewById(R.id.dialog_delete_collection_photo_confirmContainer);
            confirmContainer.setVisibility(View.VISIBLE);

        Button deleteBtn = (Button) v.findViewById(R.id.dialog_delete_collection_photo_deleteBtn);
        deleteBtn.setOnClickListener(this);

        Button cancelBtn = (Button) v.findViewById(R.id.dialog_delete_collection_photo_cancelBtn);
        cancelBtn.setOnClickListener(this);

        this.progressView = (CircularProgressView) v.findViewById(R.id.dialog_delete_collection_photo_progress);
        progressView.setVisibility(View.GONE);
    }

    private void setState(int newState) {
        switch (newState) {
            case CONFIRM_STATE:
                setCancelable(true);
                if (state == DELETE_STATE) {
                    AnimUtils.animShow(confirmContainer);
                    AnimUtils.animHide(progressView);
                }
                break;

            case DELETE_STATE:
                setCancelable(false);
                if (state == CONFIRM_STATE) {
                    AnimUtils.animShow(progressView);
                    AnimUtils.animHide(confirmContainer);
                }
                break;
        }
        state = newState;
    }

    private void notifyFailed() {
        Toast.makeText(
                getActivity(),
                getString(R.string.feedback_delete_photo_failed),
                Toast.LENGTH_SHORT).show();
    }

    /** <br> data. */

    private void initData() {
        this.service = CollectionService.getService();
        this.state = CONFIRM_STATE;
    }

    public void setDeleteInfo(Collection c, Photo p, int position) {
        collection = c;
        photo = p;
        this.position = position;
    }

    /** <br> interface. */

    // on delete collection listener.

    public interface OnDeleteCollectionListener {
        void onDeletePhotoSuccess(ChangeCollectionPhotoResult result, int position);
    }

    public void setOnDeleteCollectionListener(OnDeleteCollectionListener l) {
        listener = l;
    }

    // on click listener.

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.dialog_delete_collection_photo_deleteBtn:
                setState(DELETE_STATE);
                service.deletePhotoFromCollection(collection.id, photo.id, this);
                break;

            case R.id.dialog_delete_collection_photo_cancelBtn:
                dismiss();
                break;
        }
    }

    // on change collection photo listener.

    @Override
    public void onChangePhotoSuccess(Call<ChangeCollectionPhotoResult> call,
                                     Response<ChangeCollectionPhotoResult> response) {
        if (response.isSuccessful() && response.body() != null) {
            if (listener != null) {
                listener.onDeletePhotoSuccess(response.body(), position);
            }
            dismiss();
        } else if (Integer.parseInt(response.headers().get("X-Ratelimit-Remaining")) < 0) {
            dismiss();
            RateLimitDialog dialog = new RateLimitDialog();
            dialog.show(getFragmentManager(), null);
        } else {
            setState(CONFIRM_STATE);
            notifyFailed();
        }
    }

    @Override
    public void onChangePhotoFailed(Call<ChangeCollectionPhotoResult> call, Throwable t) {
        setState(CONFIRM_STATE);
        notifyFailed();
    }
}
