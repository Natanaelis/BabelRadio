package com.babelsoft.babelradio;

public enum Settings {
    CURRENT_RADIO_NUMBER(R.string.setting_0_key),
    RUN_SERVICE_AFTER_A2DP_CONNECTED(R.string.setting_1_key),
    AUTO_PLAY_AFTER_A2DP_CONNECTED(R.string.setting_2_key),
    A2DP_DISPLAY_UPDATE_TIME(R.string.setting_3_key),
    REBUFFERING_DELAY_TIME(R.string.setting_4_key),
    METADATA_REFRESH_TIME(R.string.setting_5_key),
    CHECK_A2DP_DEVICE_DELAY_TIME(R.string.setting_6_key);

    Settings(int set) {
    }
}
