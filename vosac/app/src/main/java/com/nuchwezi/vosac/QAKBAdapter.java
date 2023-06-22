package com.nuchwezi.vosac;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nuchwezi.xlitedatabase.DBAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

class QAKBAdapter extends BaseExpandableListAdapter  {
    private final Context context;
    private final LayoutInflater mInflater;
    private final DBAdapter dbAdapter;
    private ArrayList<String> qakbList = new ArrayList<>();
    HashMap<String, Integer> qakbColorMapping = new HashMap<>();
    QAKBActivity.QAKBRunnable runnableDeleteQAKB;

    public QAKBAdapter(Context context, JSONArray allQAKBList, DBAdapter adapter, QAKBActivity.QAKBRunnable qakbRunnable) {
        this.context = context;
        this.mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.dbAdapter = adapter;
        this.runnableDeleteQAKB = qakbRunnable;

        for(int i =0 ; i < allQAKBList.length(); i++) {
            try {
                qakbList.add(allQAKBList.getString(i));
                qakbColorMapping.put(allQAKBList.getString(i), Utility.getRandomColor());
            }catch (JSONException e){}
        }
    }

    @Override
    public int getGroupCount() {
        return qakbList.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return 1;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return qakbList.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return null;
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        GroupViewHolder holder;

        if (convertView == null) {
            holder = new GroupViewHolder();
            convertView = mInflater.inflate(R.layout.qakb_header_preview, null);
            holder.txtQAKBName = convertView.findViewById(R.id.txtQAKBName);
            holder.qakbContainer = convertView.findViewById(R.id.qakbContainer);
            convertView.setTag(holder);
        } else {
            holder = (GroupViewHolder)convertView.getTag();
        }

        final String qakbName = qakbList.get(groupPosition);


        if(qakbName != null) {
            int categoryThemeColor = qakbColorMapping.get(qakbName);
            int complimentaryColor = Utility.getContrastVersionForColor(categoryThemeColor);

            holder.qakbContainer.setBackgroundColor(categoryThemeColor);

            String actualQAKBName = qakbName.split("\\|")[0];
            holder.txtQAKBName.setText(actualQAKBName);
            holder.txtQAKBName.setTextColor(complimentaryColor);

            GradientDrawable shape = new GradientDrawable();
            shape.setCornerRadius(6);
            shape.setColor(Utility.getContrastVersionForColor(complimentaryColor));
            holder.txtQAKBName.setBackground(shape);
            holder.txtQAKBName.setPadding(5, 5, 5, 5);
        }

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        final String qakbName = qakbList.get(groupPosition);

        if ((convertView == null) || holder == null) {
            holder = new ViewHolder();

            convertView = mInflater.inflate(R.layout.qakb_items_preview, null);

            holder.itemContainer = convertView.findViewById(R.id.qakbItems);
            holder.btnDelete = convertView.findViewById(R.id.btnDeleteItem);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder)convertView.getTag();
        }


        if(qakbName != null) {
            int categoryThemeColor = qakbColorMapping.get(qakbName);
            int complimentaryColor = Utility.getContrastVersionForColor(categoryThemeColor);

            holder.itemContainer.setBackgroundColor(complimentaryColor);

            if(dbAdapter.existsDictionaryKey(qakbName)) {
                try {
                    JSONObject qakb = new JSONObject(dbAdapter.fetchDictionaryEntry(qakbName));
                    int q = 0;
                    for (Iterator<String> iter = qakb.keys(); iter.hasNext();q++ ) {
                        String qEntry = iter.next();

                        if(qEntry.equalsIgnoreCase("QRCODE"))
                            continue;

                        View qaView = mInflater.inflate(R.layout.qa_detail_preview, null);
                        try {
                            JSONArray jA = qakb.getJSONArray(qEntry);
                            StringBuilder sb = new StringBuilder();
                            ((TextView) qaView.findViewById(R.id.txtQ)).setText(String.format("\n=======||=======\nQ%d: %s\n--------\n", q + 1, qEntry));
                            for (int i = 0; i < jA.length(); i++) {
                                try {
                                    sb.append(String.format("A%d: %s\n\n", i + 1, jA.getString(i)));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            ((TextView) qaView.findViewById(R.id.txtA)).setText(sb.toString());
                            ((TextView) qaView.findViewById(R.id.txtQ)).setTextColor(categoryThemeColor);

                            holder.itemContainer.addView(qaView);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }else {
                View qaView = mInflater.inflate(R.layout.qa_detail_preview, null);
                ((TextView) qaView.findViewById(R.id.txtQ)).setText("--- NO QUESTION-ANSWER SET ---");
                ((TextView) qaView.findViewById(R.id.txtQ)).setTextColor(categoryThemeColor);
                holder.itemContainer.addView(qaView);
            }

            // so we can know which persona to item on...
            ItemIndex actIndex = new ItemIndex(groupPosition,qakbName);
            holder.btnDelete.setTag(actIndex);

            holder.btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final ItemIndex actIndex1 = (ItemIndex) v.getTag();

                    String actualQAKBName = actIndex1.item.split("\\|")[0];

                    Utility.showAlert(String.format("DELETING QAKB: %s", actualQAKBName),
                            "You really want to delete this?", R.drawable.item_remove, context, new Runnable() {
                        @Override
                        public void run() {
                            runnableDeleteQAKB.run(actIndex1.item);
                        }
                    }, null, new Runnable() {
                        @Override
                        public void run() {
                            // do nothing...
                        }
                    });

                }
            });


        }

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }

    public static class ViewHolder {
        public LinearLayout itemContainer;
        public Button btnDelete;
    }

    public static class GroupViewHolder {
        public LinearLayout qakbContainer;
        public TextView txtQAKBName;
    }

    public static class ItemIndex {
        public String item;
        public int index;
        public ItemIndex(int index, String item){
            this.index = index;
            this.item = item;
        }
    }

}
