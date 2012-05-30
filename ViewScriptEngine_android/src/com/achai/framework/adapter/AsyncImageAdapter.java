package com.achai.framework.adapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.achai.framework.R;
import com.achai.framework.image.ImageWorker;
import com.achai.framework.net.fetch.DataFetch;

public class AsyncImageAdapter extends BaseAdapter {

	// 获得布局填充服务
	private LayoutInflater mInflater;

	// 返回的布局id
	private int mResource;

	// viewhorder
	private ViewHolder mHolder;

	// 指定数据集绑定数据用的
	private int[] mTo;
	private String[] mFrom;

	// 以后优化可以考虑哟sparsearray
	private List<? extends Map<String, ?>> mData;

	private Context mContext;

	// 异步加载图片
	private ImageWorker mImageWorker;

	//
	// public AsyncImageAdapter(Context ctx, List<? extends Map<String, ?>>
	// data,
	// int resource, String[] from, int[] to) {
	// this.mData = data;
	// this.mFrom = from;
	// this.mTo = to;
	// this.mInflater = (LayoutInflater) ctx
	// .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	// this.mResource = resource;
	// }

	/**
	 * 直接在view上面绑定key的时候用 当 to为null 的时候,启动tag -key模式,这个模式的list item
	 * 必须是一个viewgroup,没有嵌套
	 * 
	 * @param ctx
	 * @param data
	 * @param resource
	 * @param to
	 */
	public AsyncImageAdapter(Context ctx, List<? extends Map<String, ?>> data,
			int resource, int[] to) {
		this.mData = data;
		this.mTo = to;
		this.mInflater = (LayoutInflater) ctx
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.mResource = resource;
		this.mContext = ctx;
		mImageWorker = new ImageWorker(ctx);
		mImageWorker.setLoadingImage(R.drawable.ic_launcher);
		
	}

	@Override
	public int getCount() {
		return mData.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return mData.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// 利用convertview 回收视图,效率提高200%
		if (convertView == null) {
			convertView = mInflater.inflate(mResource, null);
			// 利用holder 绑定 view上的tag 标签,减少搜索,提高效率
			if (mTo != null) {
				mHolder = new ViewHolder(mTo.length, mTo, convertView);
			} else {
				// list item 必须是一个viewgroup!!
				mHolder = new ViewHolder(convertView);
			}

			convertView.setTag(mHolder);
		} else {
			mHolder = (ViewHolder) convertView.getTag();
		}

		if (mTo != null) {
			bindViewsByInt(position, mHolder);
		} else {
			bindViewsByTag(position, mHolder);
		}
		return convertView;
	}

	private void bindViewsByTag(int position, ViewHolder bindHolder) {
		final Map dataSet = mData.get(position);
		if (dataSet == null) {
			return;
		}
		final List<View> bindView = bindHolder.viewHolder;
		final List<String> bindString = bindHolder.keyTags;
		for (int i = 0; i < bindView.size(); i++) {
			View v = bindView.get(i);
			String key = bindString.get(i);
			String data = (String) dataSet.get(key);
			if (data != null)
				setViewData(v, data);
		}

	}

	private void bindViewsByInt(int position, ViewHolder bindHolder) {
		final Map dataSet = mData.get(position);
		if (dataSet == null) {
			return;
		}
		final List<View> bindView = bindHolder.viewHolder;
		for (View view : bindView) {
			if (view != null) {
				Object data = null;
				// 数据的key值,直接在view上面设置
				String key = (String) view.getTag();
				if (key != null) {
					data = dataSet.get(key);
				}
				String text = data == null ? "" : data.toString();
				if (text == null) {
					text = "";
				}
				// 进行数据绑定
				if (data != null) {
					setViewData(view, text);
				}

			}
		}
	}

	private void setViewData(View view, Object data) {
		if (view instanceof TextView) {
			// 如果你要引入单选按钮等,记得,TextView要放在下面不然,会判断不了,父子继承问题
			// Note: keep the instanceof TextView check at the
			// bottom of these
			// ifs since a lot of views are TextViews (e.g.
			// CheckBoxes).
			setViewText((TextView) view, (String) data);
		} else if (view instanceof ImageView) {
			// 如果数据时int 值时直接set id
			if (data instanceof Integer) {
				setViewImage((ImageView) view, (Integer) data);
			} else {
				setViewImage((ImageView) view, (String) data);
			}
		}

	}

	private void setViewImage(ImageView view, String text) {
		if (text.startsWith("http://") ||text.startsWith("https://")) {
			mImageWorker.loadImage(text, view);
		}
	}

	private void setViewImage(ImageView view, Integer data) {
		view.setImageResource(data);
	}

	private void setViewText(TextView view, String data) {
		view.setText(data);
	}

	/**
	 * 对图片预先缩放
	 * 
	 * @param bitmap
	 * @param imageView
	 */
	private void autoScale(Bitmap bitmap, ImageView imageView) {
		bitmap = Bitmap.createScaledBitmap(bitmap, imageView.getWidth(),
				imageView.getHeight(), true);
	}

	/**
	 * 效率提升50%
	 * 
	 * @author tom_achai
	 * 
	 */
	static class ViewHolder {
		public List<View> viewHolder;
		public List<String> keyTags;

		public ViewHolder(int count, int[] to, View convertView) {
			viewHolder = new ArrayList<View>(count);
			for (int i = 0; i < count; i++) {
				final View v = convertView.findViewById(to[i]);
				viewHolder.add(v);
			}
		}

		public ViewHolder(View vg) {
			keyTags = new ArrayList<String>();
			viewHolder = new ArrayList<View>();
			setkeyByTag(vg);
		}

		private void setkeyByTag(View v) {
			if (v == null)
				return;
			String keyTag = (String) v.getTag();
			// 感觉这个会拖慢效率,看看再说
			// if (keyTag != null && !(v instanceof ViewGroup)) {
			// keyTags.add(keyTag);
			// viewHolder.add(v);
			// }

			if (keyTag != null) {
				keyTags.add(keyTag);
				viewHolder.add(v);
			}

			if (v instanceof ViewGroup) {
				ViewGroup vg = (ViewGroup) v;
				int count = vg.getChildCount();
				for (int i = 0; i < count; i++) {
					View childView = vg.getChildAt(i);
					setkeyByTag(childView);
				}
			}

		}

	}

}
