package com.doit.net.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.daimajia.swipe.SwipeLayout;
import com.daimajia.swipe.adapters.BaseSwipeAdapter;
import com.doit.net.view.AddBlacklistDialog;
import com.doit.net.view.ModifyBlackListDialog;
import com.doit.net.application.MyApplication;
import com.doit.net.bean.UeidBean;

import com.doit.net.event.AddToLocationListener;
import com.doit.net.utils.CacheManager;

import com.doit.net.utils.UtilOperator;
import com.doit.net.ucsi.R;

import java.util.List;


public class UeidListViewAdapter extends BaseSwipeAdapter {

    private Context mContext;
    private  List<UeidBean> dataList;


    public UeidListViewAdapter(Context mContext, List<UeidBean> dataList) {
        this.mContext = mContext;
        this.dataList = dataList;
    }

    @Override
    public int getSwipeLayoutResourceId(int position) {
        return R.id.swipe;
    }

    @Override
    public View generateView(int position, ViewGroup parent) {
        View v = LayoutInflater.from(mContext).inflate(R.layout.doit_layout_ueid_list_item, null);
        return v;
    }


    @Override
    public  void fillValues(int position, View convertView) {
        LinearLayout layoutItemText = convertView.findViewById(R.id.layoutItemText);
        if (position % 2 == 0) {
            layoutItemText.setBackgroundColor(mContext.getResources().getColor(R.color.deepgrey2));
        } else {
            layoutItemText.setBackgroundColor(mContext.getResources().getColor(R.color.black));
        }

        TextView index = convertView.findViewById(R.id.position);
        index.setText((position + 1) + ".");

        TextView tvContent = convertView.findViewById(R.id.tvUeidItemText);
        UeidBean resp = dataList.get(position);

        SwipeLayout swipeLayout = convertView.findViewById(R.id.swipe);



        convertView.findViewById(R.id.add_to_black).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (resp.isBlack()){
                    ModifyBlackListDialog modifyBlackListDialog = new ModifyBlackListDialog(mContext,
                            resp.getImsi(), resp.getNumber(), resp.getRemark(),false);
                    modifyBlackListDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            notifyDataSetChanged();
                            if (swipeLayout !=null){
                                swipeLayout.close();
                            }

                        }
                    });
                    modifyBlackListDialog.show();
                }else {
                    AddBlacklistDialog addBlacklistDialog = new AddBlacklistDialog(mContext, resp.getImsi(),resp.getNumber());
                    addBlacklistDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            notifyDataSetChanged();
                            if (swipeLayout !=null){
                                swipeLayout.close();
                            }
                        }
                    });
                    addBlacklistDialog.show();
                }
            }
        });

        if (CacheManager.getLocMode()) {
            convertView.findViewById(R.id.add_to_localtion).setOnClickListener(new AddToLocationListener(mContext, resp.getImsi(), resp.getType(),swipeLayout));
        } else {
            convertView.findViewById(R.id.add_to_localtion).setVisibility(View.GONE);
        }

        checkBlackWhiteList(resp, tvContent);

    }

    private void checkBlackWhiteList(UeidBean resp, TextView tvContent) {
        String type = "";
        if (resp.getType() == 0 ){
            type += "2G";
        }else {
            type += "4G";
        }
        String content = "IMSI：" + resp.getImsi() + "          " + "制式: " + UtilOperator.getOperatorNameCH(resp.getImsi())+type + "\n";


        if (!"".equals(resp.getImsi())) {

            content += resp.getRptTime() + "       " + "次数：" + resp.getRptTimes() + "       "
                    + mContext.getString(R.string.ueid_last_intensity) + resp.getSrsp();

            if (resp.isBlack()) {
                String msisdn = resp.getNumber();
                String remark = resp.getRemark();
                if (!TextUtils.isEmpty(msisdn)) {
                    content += "\n" + "手机号：" + msisdn + "           ";
                }

                if (!TextUtils.isEmpty(remark)) {
                    if (!TextUtils.isEmpty(msisdn)) {
                        content += remark;
                    } else {
                        content += "\n" + remark;
                    }
                }

                tvContent.setTextColor(MyApplication.mContext.getResources().getColor(R.color.forestgreen));

            } else {

                String msisdn = resp.getNumber();
                if (!TextUtils.isEmpty(msisdn)){
                    content += "\n" + "手机号：" + msisdn;
                }

                tvContent.setTextColor(MyApplication.mContext.getResources().getColor(R.color.white));

            }


            tvContent.setText(content);
        }


    }

    @Override
    public int getCount() {
        return dataList.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

}