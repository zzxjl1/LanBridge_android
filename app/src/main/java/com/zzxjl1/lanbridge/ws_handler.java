package com.zzxjl1.lanbridge;


import org.json.JSONArray;
import org.json.JSONObject;

public class ws_handler {

//    public static byte[] toByteArray(short[] src) {
//
//        int count = src.length;
//        byte[] dest = new byte[count << 1];
//        for (int i = 0; i < count; i++) {
//            /*小端*/
//            dest[i * 2] = (byte) (src[i] >> 8);
//            dest[i * 2 + 1] = (byte) (src[i]);
//        }
//        return dest;
//    }


    static void parse(String msg, String ip) {
        try {
            JSONObject t = new JSONObject(msg);
            String action = t.getString("action");
            switch (action) {
                case "soundwire_pcm":
                    if (!soundwire.is_allowed()) break;

                    String mode = t.getString("mode");
                    if (!mode.equals(soundwire.get_mode())) {
                        soundwire.set_mode(mode);
                    }
                    JSONArray jsonArray = t.getJSONArray("data");
                    short[] pcm = new short[jsonArray.length()];

                    for (int i = 0; i < pcm.length; i++) {
                        short temp = (short) (jsonArray.getJSONArray(i).getInt(0));
                        pcm[i] = temp;
                    }
                    if (!soundwire.is_running()) soundwire.start_soundwire_client();
                    /////soundwire.track.write(toByteArray(pcm), 0, jsonArray.length());
                    soundwire.add_to_pcm_queue(pcm);
                    break;
                case "battery_status_update":
                    JSONObject old = connections.get_battery_status(ip);
                    JSONObject data = t.getJSONObject("data");
                    if (old.getBoolean("has_battery")){
                        battery.parse_change(ip,old,data);
                    }
                    connections.set_battery_status(ip,data);
                    break;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
