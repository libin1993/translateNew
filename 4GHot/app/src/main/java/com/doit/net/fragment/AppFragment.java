package com.doit.net.fragment;

import com.doit.net.activity.Device2GParamActivity;
import com.doit.net.event.EventAdapter;
import com.doit.net.socket.ServerSocketUtils;
import com.doit.net.utils.FileUtils;
import com.doit.net.view.ClearHistoryTimeDialog;
import com.doit.net.activity.CustomFcnActivity;
import com.doit.net.activity.DeviceParamActivity;
import com.doit.net.activity.HistoryListActivity;
import com.doit.net.activity.TestActivity;
import com.doit.net.view.LicenceDialog;
import com.doit.net.activity.SystemSettingActivity;
import com.doit.net.activity.UserManageActivity;
import com.doit.net.activity.BlacklistManagerActivity;
import com.doit.net.activity.BlackBoxActivity;
import com.doit.net.utils.VersionManage;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.doit.net.base.BaseFragment;
import com.doit.net.protocol.LTESendManager;
import com.doit.net.utils.AccountManage;
import com.doit.net.utils.CacheManager;
import com.doit.net.utils.LicenceUtils;
import com.doit.net.view.MySweetAlertDialog;
import com.doit.net.utils.LogUtils;
import com.doit.net.utils.StringUtils;
import com.doit.net.utils.ToastUtils;
import com.doit.net.view.LSettingItem;
import com.doit.net.ucsi.R;

import org.xutils.view.annotation.ViewInject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class AppFragment extends BaseFragment implements EventAdapter.EventCall {

    private MySweetAlertDialog mProgressDialog;

    @ViewInject(R.id.tvLoginAccount)
    private TextView tvLoginAccount;

    @ViewInject(R.id.rl_user_manage)
    private LSettingItem rlUserManage;

    @ViewInject(R.id.view_user_manage)
    private View viewUserManage;

    @ViewInject(R.id.rl_black_box)
    private LSettingItem rlBlackBox;

    @ViewInject(R.id.view_black_box)
    private View viewBlackBox;

    @ViewInject(R.id.rl_black_list)
    private LSettingItem rlBlackList;

    @ViewInject(R.id.rl_history_data)
    private LSettingItem rlHistoryData;

    @ViewInject(R.id.view_history_data)
    private View viewHistoryData;

    @ViewInject(R.id.rl_clear_data)
    private LSettingItem rlClearData;

    @ViewInject(R.id.rl_wifi_setting)
    private LSettingItem rlWifiSetting;

    @ViewInject(R.id.rl_device_param_4g)
    private LSettingItem rlDeviceParam4G;

    @ViewInject(R.id.rl_device_param_2g)
    private LSettingItem rlDeviceParam2G;

    @ViewInject(R.id.rl_custom_fcn)
    private LSettingItem rlCustomFcn;

    @ViewInject(R.id.rl_device_upgrade)
    private LSettingItem rlDeviceUpgrade;

    @ViewInject(R.id.rl_authorize_code)
    private LSettingItem rlAuthorizeCode;

    @ViewInject(R.id.rl_local_imsi)
    private LSettingItem rlLocalImsi;

    @ViewInject(R.id.rl_version)
    private LSettingItem rlVersion;

    @ViewInject(R.id.rl_system_setting)
    private LSettingItem rlSystemSetting;

    @ViewInject(R.id.view_system_setting)
    private View viewSystemSetting;

    @ViewInject(R.id.rlTest)
    private LSettingItem rlTest;

    @ViewInject(R.id.view_test)
    private View viewTest;

    private ListView lvPackageList;
    private ArrayAdapter upgradePackageAdapter;
    private LinearLayout layoutUpgradePackage;


    //handler??????
    private final int EXPORT_SUCCESS = 0;
    private final int EXPORT_ERROR = -1;
    private final int UPGRADE_STATUS_RPT = 1;

    public AppFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.doit_layout_app, container, false);

        EventAdapter.register(EventAdapter.UPGRADE_STATUS, this);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvLoginAccount.setText(AccountManage.getCurrentLoginAccount());


        if (AccountManage.getCurrentPerLevel() >= AccountManage.PERMISSION_LEVEL2) {
            if (AccountManage.getCurrentPerLevel() >= AccountManage.PERMISSION_LEVEL3){
                rlTest.setVisibility(View.VISIBLE);
                viewTest.setVisibility(View.VISIBLE);

                rlClearData.setVisibility(View.VISIBLE);
                viewHistoryData.setVisibility(View.VISIBLE);

                rlSystemSetting.setVisibility(View.VISIBLE);
                viewSystemSetting.setVisibility(View.VISIBLE);
            }
            rlUserManage.setVisibility(View.VISIBLE);
            viewUserManage.setVisibility(View.VISIBLE);


            rlBlackBox.setVisibility(View.VISIBLE);
            viewBlackBox.setVisibility(View.VISIBLE);


            rlBlackList.setVisibility(View.VISIBLE);

        }

        rlUserManage.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(LSettingItem item) {
                if (!CacheManager.checkDevice(getContext()))
                    return;
                startActivity(new Intent(getActivity(), UserManageActivity.class));
            }
        });


        rlBlackBox.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(LSettingItem item) {
                if (!CacheManager.checkDevice(getContext()))
                    return;
                startActivity(new Intent(getActivity(), BlackBoxActivity.class));
            }
        });

        rlBlackList.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(LSettingItem item) {
                startActivity(new Intent(getActivity(), BlacklistManagerActivity.class));
            }
        });

        rlHistoryData.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(LSettingItem item) {
                startActivity(new Intent(getActivity(), HistoryListActivity.class));
            }
        });

        rlClearData.setmOnLSettingItemClick(clearHistoryListener);



        rlWifiSetting.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(LSettingItem item) {
                startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));
            }
        });

        rlDeviceParam4G.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(LSettingItem item) {
                if (!CacheManager.checkDevice(getActivity())) {
                    return;
                }
                startActivity(new Intent(getActivity(), DeviceParamActivity.class));
            }
        });

        rlDeviceParam2G.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(LSettingItem item) {
                if (!CacheManager.checkDevice(getActivity()))
                    return;
                startActivity(new Intent(getActivity(), Device2GParamActivity.class));
            }
        });

        rlCustomFcn.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(LSettingItem item) {
                if (!CacheManager.checkDevice(getActivity()))
                    return;

                startActivity(new Intent(getActivity(), CustomFcnActivity.class));
            }
        });

        rlDeviceUpgrade.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(LSettingItem item) {
                showDeviceInfoDialog();
            }
        });

        rlAuthorizeCode.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(LSettingItem item) {
                if (!CacheManager.checkDevice(getActivity()))
                    return;

                if (!TextUtils.isEmpty(LicenceUtils.authorizeCode)) {
                    LicenceDialog licenceDialog = new LicenceDialog(getActivity());
                    licenceDialog.show();
                } else {
                    ToastUtils.showMessage("??????????????????????????????");
                }

            }
        });

        rlTest.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(LSettingItem item) {
                startActivity(new Intent(getActivity(), TestActivity.class));
            }
        });


        String imsi = getImsi();
        rlLocalImsi.setRightText(imsi);
        rlLocalImsi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //???????????????????????????
                ClipboardManager cm = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                // ?????????????????????ClipData
                ClipData mClipData = ClipData.newPlainText("Label", imsi);
                // ???ClipData?????????????????????????????????
                cm.setPrimaryClip(mClipData);
            }
        });


        rlVersion.setRightText(VersionManage.getVersionName(getContext()));

        rlSystemSetting.setmOnLSettingItemClick(new LSettingItem.OnLSettingItemClick() {
            @Override
            public void click(LSettingItem item) {
                startActivity(new Intent(getActivity(), SystemSettingActivity.class));
            }
        });

        initProgressDialog();

    }

    private void initProgressDialog() {
        mProgressDialog = new MySweetAlertDialog(getContext(), MySweetAlertDialog.PROGRESS_TYPE);
        mProgressDialog.setTitleText("???????????????????????????????????????...");
        mProgressDialog.setCancelable(false);
    }

    private void showDeviceInfoDialog() {
        if (!CacheManager.checkDevice(getContext()))
            return;

        final View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.layout_device_info, null);
        TextView tvDeviceIP = dialogView.findViewById(R.id.tvDeviceIP);
        tvDeviceIP.setText(ServerSocketUtils.REMOTE_4G_HOST + " | " + ServerSocketUtils.REMOTE_2G_HOST);

        TextView tvHwVersion = dialogView.findViewById(R.id.tvHwVersion);

        tvHwVersion.setText(CacheManager.getLteEquipConfig().getHw());

        TextView tvSwVersion = dialogView.findViewById(R.id.tvSwVersion);
        String softwareVersion = "4G:" + CacheManager.getLteEquipConfig().getSw();
        if (!TextUtils.isEmpty(CacheManager.GSMSoftwareVersion)) {
            softwareVersion += "\nGSM:" + CacheManager.GSMSoftwareVersion;
        }

        if (!TextUtils.isEmpty(CacheManager.CDMASoftwareVersion)) {
            softwareVersion += "\nCDMA:" + CacheManager.CDMASoftwareVersion;
        }
        tvSwVersion.setText(softwareVersion);



        Button btDeviceUpgrade = dialogView.findViewById(R.id.btDeviceUpgrade);
        btDeviceUpgrade.setOnClickListener(upgradeListener);
        lvPackageList = dialogView.findViewById(R.id.lvPackageList);
        layoutUpgradePackage = dialogView.findViewById(R.id.layoutUpgradePackage);

        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
        dialog.setView(dialogView);
        dialog.setCancelable(true);
        dialog.show();
    }

    @SuppressLint("MissingPermission")
    private String getImsi() {
        TelephonyManager telManager = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        return StringUtils.defaultIfBlank(telManager.getSubscriberId(), getString(R.string.no_sim_card));
    }

    @SuppressLint("MissingPermission")
    private String getImei() {
        TelephonyManager telManager = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        return StringUtils.defaultIfBlank(telManager.getDeviceId(), getString(R.string.no));
    }

    LSettingItem.OnLSettingItemClick clearHistoryListener = new LSettingItem.OnLSettingItemClick() {
        @Override
        public void click(LSettingItem item) {
            ClearHistoryTimeDialog clearHistoryTimeDialog = new ClearHistoryTimeDialog(getActivity());
            clearHistoryTimeDialog.show();
        }
    };


    private String getPackageMD5(String FilePath) {
        BigInteger bi = null;
        try {
            byte[] buffer = new byte[8192];
            int len = 0;
            MessageDigest md = MessageDigest.getInstance("MD5");
            File f = new File(FilePath);
            FileInputStream fis = new FileInputStream(f);
            while ((len = fis.read(buffer)) != -1) {
                md.update(buffer, 0, len);
            }
            fis.close();
            byte[] b = md.digest();
            bi = new BigInteger(1, b);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bi.toString(16);
    }

    View.OnClickListener upgradeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String UPGRADE_PACKAGE_PATH = "upgrade/";

            File file = new File(FileUtils.ROOT_PATH + UPGRADE_PACKAGE_PATH);
            if (!file.exists()) {
                ToastUtils.showMessageLong("???????????????????????????????????????????????????\"????????????/" + FileUtils.ROOT_DIRECTORY + "/upgrade\"?????????");
                return;
            }

            File[] files = file.listFiles();
            if (files == null || files.length == 0) {
                ToastUtils.showMessageLong("???????????????????????????????????????????????????\"????????????/" + FileUtils.ROOT_DIRECTORY + "/upgrade\"?????????");
                return;
            }

            List<String> fileList = new ArrayList<>();
            String tmpFileName = "";
            for (int i = 0; i < files.length; i++) {
                tmpFileName = files[i].getName();
                if (tmpFileName.endsWith(".tgz"))
                    fileList.add(tmpFileName);
            }
            if (fileList.size() == 0) {
                ToastUtils.showMessageLong("????????????????????????????????????\".tgz\"??????????????????");
                return;
            }

            layoutUpgradePackage.setVisibility(View.VISIBLE);
            upgradePackageAdapter = new ArrayAdapter<String>(getContext(), R.layout.comman_listview_text, fileList);
            lvPackageList.setAdapter(upgradePackageAdapter);
            lvPackageList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final String choosePackage = fileList.get(position);
                    LogUtils.log("??????????????????" + choosePackage);

                    new MySweetAlertDialog(getContext(), MySweetAlertDialog.WARNING_TYPE)
                            .setTitleText("??????")
                            .setContentText("????????????????????????" + choosePackage + ", ??????????????????")
                            .setCancelText(getContext().getString(R.string.cancel))
                            .setConfirmText(getContext().getString(R.string.sure))
                            .showCancelButton(true)
                            .setConfirmClickListener(new MySweetAlertDialog.OnSweetClickListener() {
                                @Override
                                public void onClick(MySweetAlertDialog sweetAlertDialog) {
                                    String md5 = getPackageMD5(FileUtils.ROOT_PATH + UPGRADE_PACKAGE_PATH + choosePackage);
                                    if ("".equals(md5)) {
                                        ToastUtils.showMessage("????????????????????????????????????");
                                        sweetAlertDialog.dismiss();
                                    } else {
                                        String command = UPGRADE_PACKAGE_PATH + choosePackage + "#" + md5;
                                        LTESendManager.systemUpgrade(command);
                                        mProgressDialog.show();
                                        sweetAlertDialog.dismiss();
                                    }
                                }
                            }).show();

                }
            });
        }
    };

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == EXPORT_SUCCESS) {
                new SweetAlertDialog(getActivity(), SweetAlertDialog.SUCCESS_TYPE)
                        .setTitleText("????????????")
                        .setContentText("??????????????????" + msg.obj)
                        .show();
            } else if (msg.what == EXPORT_ERROR) {
                new SweetAlertDialog(getActivity(), SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("????????????")
                        .setContentText("???????????????" + msg.obj)
                        .show();
            } else if (msg.what == UPGRADE_STATUS_RPT) {
                if (mProgressDialog != null)
                    mProgressDialog.dismiss();
            }
        }
    };




    @Override
    public void call(String key, Object val) {
        if (key.equals(EventAdapter.UPGRADE_STATUS)) {
            mHandler.sendEmptyMessage(UPGRADE_STATUS_RPT);
        }
    }
}
