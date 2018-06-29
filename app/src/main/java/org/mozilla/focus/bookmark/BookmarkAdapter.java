package org.mozilla.focus.bookmark;

import android.arch.paging.PagedListAdapter;
import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.mozilla.focus.R;
import org.mozilla.focus.fragment.PanelFragmentStatusListener;
import org.mozilla.focus.persistence.BookmarkModel;
import org.mozilla.focus.site.SiteItemViewHolder;
import org.mozilla.focus.telemetry.TelemetryWrapper;

import java.util.Objects;

public class BookmarkAdapter extends PagedListAdapter<BookmarkModel, SiteItemViewHolder> {
    private BookmarkPanelListener listener;

    public BookmarkAdapter(BookmarkPanelListener listener) {
        super(diffCallback);
        this.listener = listener;
    }

    @NonNull
    @Override
    public SiteItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_website, parent, false);
        return new SiteItemViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SiteItemViewHolder holder, int position) {
        final BookmarkModel item = getItem(position);
        if (item == null) {
            holder.rootView.setTag("");
            holder.textMain.setText("");
            holder.textSecondary.setText("");
            return;
        }

        holder.rootView.setTag(item.getId());
        holder.textMain.setText(item.getTitle());
        holder.textSecondary.setText(item.getUrl());
        holder.rootView.setOnClickListener(v -> {
            listener.onItemClicked(item.getUrl());
        });
        final PopupMenu popupMenu = new PopupMenu(holder.btnMore.getContext(), holder.btnMore);
        popupMenu.setOnMenuItemClickListener(menuItem -> {
            if (menuItem.getItemId() == R.id.remove) {
                listener.onItemDeleted(item);
            }
            if (menuItem.getItemId() == R.id.edit) {
                listener.onItemEdited(item);
            }
            return false;
        });
        popupMenu.inflate(R.menu.menu_bookmarks);
        holder.btnMore.setOnClickListener(v -> {
            popupMenu.show();
            TelemetryWrapper.showBookmarkContextMenu();
        });
    }

    public interface BookmarkPanelListener extends PanelFragmentStatusListener {
        void onItemClicked(String url);

        void onItemDeleted(BookmarkModel bookmark);

        void onItemEdited(BookmarkModel bookmark);
    }

    private static final DiffUtil.ItemCallback<BookmarkModel> diffCallback = new DiffUtil.ItemCallback<BookmarkModel>() {
        @Override
        public boolean areItemsTheSame(BookmarkModel oldItem, BookmarkModel newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(BookmarkModel oldItem, BookmarkModel newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId())
                    && Objects.equals(oldItem.getTitle(), newItem.getTitle())
                    && Objects.equals(oldItem.getUrl(), newItem.getUrl());
        }
    };
}
