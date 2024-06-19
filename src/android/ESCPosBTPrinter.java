package nsc.cordova.escposbtprinter;

import org.apache.cordova.CordovaPlugin;

import java.io.UnsupportedEncodingException;

import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.rt.printerlibrary.bean.BluetoothEdrConfigBean;
import com.rt.printerlibrary.bean.LableSizeBean;
import com.rt.printerlibrary.bean.Position;
import com.rt.printerlibrary.bean.PrinterStatusBean;
import com.rt.printerlibrary.cmd.Cmd;
import com.rt.printerlibrary.cmd.CpclFactory;
import com.rt.printerlibrary.cmd.EscCmd;
import com.rt.printerlibrary.cmd.EscFactory;
import com.rt.printerlibrary.cmd.PinFactory;
import com.rt.printerlibrary.connect.PrinterInterface;
import com.rt.printerlibrary.enumerate.BarcodeStringPosition;
import com.rt.printerlibrary.enumerate.BarcodeType;
import com.rt.printerlibrary.enumerate.BmpPrintMode;
import com.rt.printerlibrary.enumerate.CommonEnum;
import com.rt.printerlibrary.enumerate.ConnectStateEnum;
import com.rt.printerlibrary.enumerate.PrintDirection;
import com.rt.printerlibrary.enumerate.PrintRotation;
import com.rt.printerlibrary.enumerate.PrinterAskStatusEnum;
import com.rt.printerlibrary.enumerate.QrcodeEccLevel;
import com.rt.printerlibrary.enumerate.SettingEnum;
import com.rt.printerlibrary.enumerate.TscFontTypeEnum;
import com.rt.printerlibrary.exception.SdkException;
import com.rt.printerlibrary.factory.cmd.CmdFactory;
import com.rt.printerlibrary.factory.connect.BluetoothFactory;
import com.rt.printerlibrary.factory.connect.PIFactory;
import com.rt.printerlibrary.factory.connect.UsbFactory;
import com.rt.printerlibrary.factory.connect.WiFiFactory;
import com.rt.printerlibrary.factory.printer.LabelPrinterFactory;
import com.rt.printerlibrary.factory.printer.PinPrinterFactory;
import com.rt.printerlibrary.factory.printer.PrinterFactory;
import com.rt.printerlibrary.factory.printer.ThermalPrinterFactory;
import com.rt.printerlibrary.factory.printer.UniversalPrinterFactory;
import com.rt.printerlibrary.observer.PrinterObserver;
import com.rt.printerlibrary.observer.PrinterObserverManager;
import com.rt.printerlibrary.printer.RTPrinter;
import com.rt.printerlibrary.setting.BarcodeSetting;
import com.rt.printerlibrary.setting.BitmapSetting;
import com.rt.printerlibrary.setting.CommonSetting;
import com.rt.printerlibrary.setting.TextSetting;
import com.rt.printerlibrary.utils.ConnectListener;
import com.rt.printerlibrary.utils.FuncUtils;
import com.rt.printerlibrary.utils.PrintListener;
import com.rt.printerlibrary.utils.PrintStatusCmd;
import com.rt.printerlibrary.utils.PrinterStatusPareseUtils;
import com.rt.printerlibrary.enumerate.ESCFontTypeEnum;

public class ESCPosBTPrinter extends CordovaPlugin {
    private CallbackContext callbackContext;

    private RTPrinter rtPrinter;
    private String printStr;
    private String deviceAddress;

    private String mChartsetName;
    private int lineSpacing;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;

        if (action.equals("print")) {
            String message = args.getString(0);
            this.print(message);
            return true;
        } else if (action.equals("connect")) {
            deviceAddress = args.getString(0);
            this.connect();
            return true;
        } else if (action.equals("disconnect")) {
            this.doDisConnect();
            return true;
        } else if (action.equals("printTemplate")) {
            this.EsctemplatePrint2();
            return true;
        } else if (action.equals("printWhitFormat")) {
            this.printWhitFormat(args.getJSONObject(0));
            return true;
        } else if (action.equals("printBarCode")) {
            this.printBarCode(args.getJSONObject(0));
            return true;
        } else if (action.equals("printImage")) {
            this.printImage(args.getJSONObject(0));
            return true;
        } else if (action.equals("getConnectState")) {
            this.getConnectState();
            return true;
        }

        return false;
    }

    private void getConnectState() {
        try {
            callbackContext.success("ConnectState: " + rtPrinter.getConnectState().toString());
        } catch (Exception e) {
            callbackContext.error("getConnectState >> " + e.toString());
        }
    }

    private void print(String message) {
        if (message != null && message.length() > 0) {
            // Lógica para imprimir
            printStr = message;
            this.escPrint();

        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }

    private void connect() {
        try {
            UniversalPrinterFactory printerFactory = new UniversalPrinterFactory();
            rtPrinter = printerFactory.create();

            this.mChartsetName = "UTF-8";
            this.lineSpacing = 30;

            this.doConnect();
        } catch (Exception e) {
            callbackContext.error("connect >> " + e.toString());
        }
    }

    private void doDisConnect() {
        try {
            rtPrinter.disConnect();
            callbackContext.success("Disconnected!!!.");
        } catch (Exception e) {
            callbackContext.error("doDisConnect >> " + e.toString());
        }
    }

    private void printBarCode(JSONObject config) {
        try {
            CmdFactory escFac = new EscFactory();
            Cmd escCmd = escFac.create();
            String text = "N0123456789";
            BarcodeType barcodeType = BarcodeType.CODE93;

            BarcodeSetting barcodeSetting = applyBarcodeSettings(config);

            if (config.has("setBarcodeType")) {
                String _configVal = config.getString("setBarcodeType");
                if (_configVal.equals("UPC_A"))
                    barcodeType = BarcodeType.UPC_A;
                if (_configVal.equals("UPC_E"))
                    barcodeType = BarcodeType.UPC_E;
                if (_configVal.equals("EAN13"))
                    barcodeType = BarcodeType.EAN13;
                if (_configVal.equals("EAN14"))
                    barcodeType = BarcodeType.EAN14;
                if (_configVal.equals("EAN8"))
                    barcodeType = BarcodeType.EAN8;
                if (_configVal.equals("CODE39"))
                    barcodeType = BarcodeType.CODE39;
                if (_configVal.equals("ITF"))
                    barcodeType = BarcodeType.ITF;
                if (_configVal.equals("CODABAR"))
                    barcodeType = BarcodeType.CODABAR;
                if (_configVal.equals("CODE93"))
                    barcodeType = BarcodeType.CODE93;
                if (_configVal.equals("CODE128"))
                    barcodeType = BarcodeType.CODE128;
                if (_configVal.equals("GS1"))
                    barcodeType = BarcodeType.GS1;
                if (_configVal.equals("QR_CODE"))
                    barcodeType = BarcodeType.QR_CODE;
            }
            if (config.has("text")) {
                text = config.getString("text");
            }

            escCmd.append(escCmd.getBarcodeCmd(barcodeType, barcodeSetting, text));

            rtPrinter.writeMsgAsync(escCmd.getAppendCmds());

            callbackContext.success("Print " + text + " Barcode.");

        } catch (Exception e) {
            callbackContext.error("printWhitFormat >> " + e.toString());
        }
    }

    private BarcodeSetting applyBarcodeSettings(JSONObject config) {
        BarcodeSetting barcodeSetting = new BarcodeSetting();
        barcodeSetting.setBarcodeStringPosition(BarcodeStringPosition.NONE);
        barcodeSetting.setHeightInDot(120);
        barcodeSetting.setBarcodeWidth(3);

        try {
            if (config.has("setBarcodeStringPosition")) {
                String _configVal = config.getString("setBarcodeStringPosition");
                if (_configVal.equals("NONE"))
                    barcodeSetting.setBarcodeStringPosition(BarcodeStringPosition.NONE);
                if (_configVal.equals("ABOVE_BARCODE"))
                    barcodeSetting.setBarcodeStringPosition(BarcodeStringPosition.ABOVE_BARCODE);
                if (_configVal.equals("BELOW_BARCODE"))
                    barcodeSetting.setBarcodeStringPosition(BarcodeStringPosition.BELOW_BARCODE);
                if (_configVal.equals("ABOVE_BELOW_BARCODE"))
                    barcodeSetting.setBarcodeStringPosition(BarcodeStringPosition.ABOVE_BELOW_BARCODE);
            }
            if (config.has("setHeightInDot")) {
                int _configVal = config.getInt("setHeightInDot");
                barcodeSetting.setHeightInDot(_configVal);
            }
            if (config.has("setBarcodeWidth")) {
                int _configVal = config.getInt("setBarcodeWidth");
                barcodeSetting.setBarcodeWidth(_configVal);
            }
            if (config.has("setPrintRotation")) {
                String _configVal = config.getString("setPrintRotation");
                if (_configVal.equals("Rotate0"))
                    barcodeSetting.setPrintRotation(PrintRotation.Rotate0);
                if (_configVal.equals("Rotate90"))
                    barcodeSetting.setPrintRotation(PrintRotation.Rotate90);
                if (_configVal.equals("Rotate180"))
                    barcodeSetting.setPrintRotation(PrintRotation.Rotate180);
                if (_configVal.equals("Rotate270"))
                    barcodeSetting.setPrintRotation(PrintRotation.Rotate270);
            }
            /*
             * if (config.has("setEscBarcodFont")) {
             * String _configVal = config.getString("setEscBarcodFont");
             * if (_configVal.equals("BARFONT_A_12x24"))
             * barcodeSetting.setEscBarcodFont(ESCBarcodeFontTypeEnum.BARFONT_A_12x24);
             * if (_configVal.equals("BARFONT_B_9x17"))
             * barcodeSetting.setEscBarcodFont(ESCBarcodeFontTypeEnum.BARFONT_B_9x17);
             * }
             */
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            callbackContext.error("printWhitFormat >> " + e.toString());
        }

        return barcodeSetting;
    }

    private void printImage(JSONObject config) {
        try {
            String b64Img = "";
            int LimitWidth = 208;
            if (config.has("data")) {
                b64Img = config.getString("data");
            }
            if (config.has("setBimtapLimitWidth")) {
                LimitWidth = config.getInt("setBimtapLimitWidth");
            }

            CmdFactory escFac = new EscFactory();
            Cmd escCmd = escFac.create();
            Bitmap bitLogo = convertBase64ToBitmap(b64Img);
            BitmapSetting bitmapSetting = new BitmapSetting();

            if (bitLogo != null) {
                bitmapSetting.setBimtapLimitWidth(LimitWidth);
                bitmapSetting.setBmpPrintMode(BmpPrintMode.MODE_SINGLE_COLOR);
                escCmd.append(escCmd.getBitmapCmd(bitmapSetting, bitLogo));
            }
            rtPrinter.writeMsgAsync(escCmd.getAppendCmds());

            callbackContext.success("Print Image.");

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            callbackContext.error("printWhitFormat >> " + e.toString());
        }
    }

    private void printWhitFormat(JSONObject config) {
        try {
            CmdFactory escFac = new EscFactory();
            Cmd escCmd = escFac.create();
            String text = "ESCP Printer!!!";
            String[] lines;
            int charsByLine = 30;
            escCmd.setChartsetName(mChartsetName);

            CommonSetting commonSetting = new CommonSetting();
            TextSetting textSetting = applyTextSettings(config);
            if (config.has("lineSpacing")) {
                this.lineSpacing = config.getInt("lineSpacing");
            }
            if (config.has("charsByLine")) {
                charsByLine = config.getInt("charsByLine");
            }
            if (config.has("text")) {
                text = config.getString("text");
            }

            commonSetting.setEscLineSpacing(lineSpacing);
            escCmd.append(escCmd.getCommonSettingCmd(commonSetting));

            lines = text.split("@escp_cl@");

            int k = 0;
            int f = lines.length;

            int subLines = 0;

            while (k < f) {
                while (lines[k].length() > charsByLine) {
                    String subLinea = lines[k].substring(0, charsByLine);
                    lines[k] = lines[k].substring(charsByLine, lines[k].length());
                    escCmd.append(escCmd.getTextCmd(textSetting, subLinea));
                    escCmd.append(escCmd.getLFCRCmd());
                    subLines++;
                }
                escCmd.append(escCmd.getTextCmd(textSetting, lines[k]));
                escCmd.append(escCmd.getLFCRCmd());
                k++;
            }

            rtPrinter.writeMsgAsync(escCmd.getAppendCmds());

            callbackContext.success(
                    "Print " + lines.length + " lines. Sublines: " + subLines + " lines. charsByLine: " + charsByLine
                            + " chars. mChartsetName: " + this.mChartsetName);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            callbackContext.error("printWhitFormat >> " + e.toString());
        }
    }

    private TextSetting applyTextSettings(JSONObject config) {
        TextSetting textSetting = new TextSetting();
        try {
            Position txtposition = new Position(0, 0);
            txtposition.x = 160; // 160*0.125=20mm
            textSetting.setTxtPrintPosition(txtposition);

            if (config.has("setAlign")) {
                String _configVal = config.getString("setAlign");
                if (_configVal.equals("ALIGN_LEFT"))
                    textSetting.setAlign(CommonEnum.ALIGN_LEFT);
                if (_configVal.equals("ALIGN_MIDDLE"))
                    textSetting.setAlign(CommonEnum.ALIGN_MIDDLE);
                if (_configVal.equals("ALIGN_RIGHT"))
                    textSetting.setAlign(CommonEnum.ALIGN_RIGHT);
            }
            if (config.has("setEscFontType")) {
                String _configVal = config.getString("setEscFontType");
                if (_configVal.equals("FONT_A_12x24"))
                    textSetting.setEscFontType(ESCFontTypeEnum.FONT_A_12x24);
                if (_configVal.equals("FONT_B_9x24"))
                    textSetting.setEscFontType(ESCFontTypeEnum.FONT_B_9x24);
                if (_configVal.equals("FONT_C_9x17"))
                    textSetting.setEscFontType(ESCFontTypeEnum.FONT_C_9x17);
                if (_configVal.equals("FONT_D_8x16"))
                    textSetting.setEscFontType(ESCFontTypeEnum.FONT_D_8x16);
            }
            if (config.has("setUnderline")) {
                String _configVal = config.getString("setUnderline");
                if (_configVal.equals("Enable"))
                    textSetting.setUnderline(SettingEnum.Enable);
                if (_configVal.equals("Disable"))
                    textSetting.setUnderline(SettingEnum.Disable);
            }
            if (config.has("setBold")) {
                String _configVal = config.getString("setBold");
                if (_configVal.equals("Enable"))
                    textSetting.setBold(SettingEnum.Enable);
                if (_configVal.equals("Disable"))
                    textSetting.setBold(SettingEnum.Disable);
            }
            if (config.has("setDoubleHeight")) {
                String _configVal = config.getString("setDoubleHeight");
                if (_configVal.equals("Enable"))
                    textSetting.setDoubleHeight(SettingEnum.Enable);
                if (_configVal.equals("Disable"))
                    textSetting.setDoubleHeight(SettingEnum.Disable);
            }
            if (config.has("setDoubleWidth")) {
                String _configVal = config.getString("setDoubleWidth");
                if (_configVal.equals("Enable"))
                    textSetting.setDoubleWidth(SettingEnum.Enable);
                if (_configVal.equals("Disable"))
                    textSetting.setDoubleWidth(SettingEnum.Disable);
            }
            if (config.has("setIsAntiWhite")) {
                String _configVal = config.getString("setIsAntiWhite");
                if (_configVal.equals("Enable"))
                    textSetting.setIsAntiWhite(SettingEnum.Enable);
                if (_configVal.equals("Disable"))
                    textSetting.setIsAntiWhite(SettingEnum.Disable);
            }
            if (config.has("setIsEscSmallCharactor")) {
                String _configVal = config.getString("setIsEscSmallCharactor");
                if (_configVal.equals("Enable"))
                    textSetting.setIsEscSmallCharactor(SettingEnum.Enable);
                if (_configVal.equals("Disable"))
                    textSetting.setIsEscSmallCharactor(SettingEnum.Disable);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            callbackContext.error("printWhitFormat >> " + e.toString());
        }

        return textSetting;
    }

    private void escPrint() {
        try {
            TextSetting textSetting = new TextSetting();
            CmdFactory escFac = new EscFactory();
            Cmd escCmd = escFac.create();
            escCmd.setChartsetName(mChartsetName);

            escCmd.append(escCmd.getTextCmd(textSetting, printStr));
            escCmd.append(escCmd.getLFCRCmd());

            rtPrinter.writeMsg(escCmd.getAppendCmds());
        } catch (Exception e) {
            callbackContext.error("escPrint >> " + e.toString());
        }
    }

    private void doConnect() {
        try {
            BluetoothDevice device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);
            BluetoothEdrConfigBean bluetoothEdrConfigBean = new BluetoothEdrConfigBean(device);
            connectBluetooth(bluetoothEdrConfigBean);
        } catch (Exception e) {
            callbackContext.error("doConnect >> " + e.toString());
        }
    }

    private void connectBluetooth(BluetoothEdrConfigBean bluetoothEdrConfigBean) {
        try {
            PIFactory piFactory = new BluetoothFactory();
            PrinterInterface printerInterface = piFactory.create();
            printerInterface.setConfigObject(bluetoothEdrConfigBean);
            rtPrinter.setPrinterInterface(printerInterface);
            rtPrinter.connect(bluetoothEdrConfigBean);
        } catch (Exception e) {
            callbackContext.error("connectBluetooth >> " + e.toString());
        } finally {
            callbackContext.success("Success!!");
        }
    }

    public void EsctemplatePrint2() {

        try {
            CommonSetting commonSetting = new CommonSetting();
            CmdFactory EscFac = new EscFactory();
            Cmd escCmd = EscFac.create();

            BarcodeSetting barcodeSetting = new BarcodeSetting();
            TextSetting textSetting = new TextSetting();
            // 版面居中
            commonSetting.setAlign(CommonEnum.ALIGN_MIDDLE);
            escCmd.append(escCmd.getCommonSettingCmd(commonSetting));

            // 第一个二维码
            barcodeSetting.setQrcodeDotSize(4); // 放大倍数为 4
            barcodeSetting.setQrcodeEccLevel(QrcodeEccLevel.M); // 纠错
            escCmd.append(escCmd.getBarcodeCmd(BarcodeType.QR_CODE, barcodeSetting,
                    "http://weixin.qq.com/r/MSqqsnjEoiXdrTzR938j"));
            escCmd.append(escCmd.getLFCmd());

            escCmd.append(escCmd.getTextCmd(textSetting, "LA: 7FRESH-DZ-00188")); // 后面不加 LF，保证后面的内容在同一行
            textSetting.setDoubleHeight(SettingEnum.Enable); // 字体倍高
            escCmd.append(escCmd.getTextCmd(textSetting, "LB: 5925"));
            escCmd.append(escCmd.getLFCmd()); // 打印一行
            escCmd.append(escCmd.getLFCmd()); // 空一行

            textSetting.setDoubleHeight(SettingEnum.Enable); // 倍高字体
            textSetting.setDoubleWidth(SettingEnum.Enable); // 倍宽字体
            escCmd.append(escCmd.getTextCmd(textSetting, "LC: ABCDEFG 2"));
            escCmd.append(escCmd.getLFCmd());

            // 正常字体，打印一条水平线
            textSetting.setDoubleHeight(SettingEnum.Disable);
            textSetting.setDoubleWidth(SettingEnum.Disable);
            escCmd.append(escCmd.getTextCmd(textSetting, "------------------------------------------------"));
            escCmd.append(escCmd.getLFCmd());

            // 左对齐，倍高字体
            commonSetting.setAlign(CommonEnum.ALIGN_LEFT);
            escCmd.append(escCmd.getCommonSettingCmd(commonSetting));
            textSetting.setDoubleHeight(SettingEnum.Enable);
            escCmd.append(escCmd.getTextCmd(textSetting, "LD:     ZJHGF"));
            escCmd.append(escCmd.getLFCmd());

            // 水平线
            textSetting.setDoubleHeight(SettingEnum.Disable);
            textSetting.setDoubleWidth(SettingEnum.Disable);
            escCmd.append(escCmd.getTextCmd(textSetting, "------------------------------------------------"));
            escCmd.append(escCmd.getLFCmd());

            commonSetting.setAlign(CommonEnum.ALIGN_LEFT);
            escCmd.append(escCmd.getCommonSettingCmd(commonSetting));
            textSetting.setDoubleHeight(SettingEnum.Enable);
            escCmd.append(escCmd.getTextCmd(textSetting, "LE:     4D5F         |        1671  1671"));
            escCmd.append(escCmd.getLFCmd());
            escCmd.append(escCmd.getTextCmd(textSetting, "LF:                    |        1671  1671"));
            escCmd.append(escCmd.getLFCmd());

            textSetting.setDoubleHeight(SettingEnum.Disable);
            textSetting.setDoubleWidth(SettingEnum.Disable);
            escCmd.append(escCmd.getTextCmd(textSetting, "------------------------------------------------"));
            escCmd.append(escCmd.getLFCmd());

            escCmd.append(escCmd.getTextCmd(textSetting, "LG: -saasNNNNN999999999999"));
            escCmd.append(escCmd.getLFCmd());
            escCmd.append(escCmd.getTextCmd(textSetting, "                                      LH"));
            escCmd.append(escCmd.getLFCmd());
            escCmd.append(escCmd.getTextCmd(textSetting, "LI:Tdzzhuangss       TYUIdzzhuangss"));
            escCmd.append(escCmd.getLFCmd());
            escCmd.append(escCmd.getTextCmd(textSetting, "LJ:GH2019-06-05 10:31:24"));
            escCmd.append(escCmd.getLFCmd());
            escCmd.append(escCmd.getLFCmd());
            escCmd.append(escCmd.getTextCmd(textSetting, "------------------------------------------------"));
            escCmd.append(escCmd.getLFCmd());
            escCmd.append(escCmd.getLFCmd());
            escCmd.append(escCmd.getLFCmd());

            // 居中对齐
            commonSetting.setAlign(CommonEnum.ALIGN_MIDDLE);
            escCmd.append(escCmd.getCommonSettingCmd(commonSetting));

            // 打印图标
            /*
             * Bitmap bitLogo = BitmapFactory.decodeResource(context.getResources(),
             * R.mipmap.freshlogo);
             * if (bitLogo != null) {
             * BitmapSetting bitmapSetting = new BitmapSetting();
             * bitmapSetting.setBimtapLimitWidth(208);
             * bitmapSetting.setBmpPrintMode(BmpPrintMode.MODE_SINGLE_COLOR);
             * escCmd.append(escCmd.getBitmapCmd(bitmapSetting, bitLogo));
             * }
             * 
             */

            Bitmap bitLogo = convertBase64ToBitmap(
                    "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");
            if (bitLogo != null) {
                BitmapSetting bitmapSetting = new BitmapSetting();
                bitmapSetting.setBimtapLimitWidth(208);
                bitmapSetting.setBmpPrintMode(BmpPrintMode.MODE_SINGLE_COLOR);
                escCmd.append(escCmd.getBitmapCmd(bitmapSetting, bitLogo));
            }

            textSetting.setDoubleHeight(SettingEnum.Enable);
            textSetting.setDoubleWidth(SettingEnum.Enable);
            escCmd.append(escCmd.getTextCmd(textSetting, "NSC BTPRINT"));
            escCmd.append(escCmd.getLFCmd());

            barcodeSetting.setBarcodeStringPosition(BarcodeStringPosition.BELOW_BARCODE);
            barcodeSetting.setHeightInDot(120);
            barcodeSetting.setBarcodeWidth(2);
            escCmd.append(escCmd.getBarcodeCmd(BarcodeType.CODE128, barcodeSetting, "670006394135"));

            textSetting.setDoubleHeight(SettingEnum.Disable);
            textSetting.setDoubleWidth(SettingEnum.Disable);
            commonSetting.setAlign(CommonEnum.ALIGN_LEFT);
            escCmd.append(escCmd.getCommonSettingCmd(commonSetting));
            escCmd.append(escCmd.getTextCmd(textSetting, "------------------------------------------------"));
            escCmd.append(escCmd.getLFCmd());

            escCmd.append(escCmd.getTextCmd(textSetting, "L1: 670006394135"));
            escCmd.append(escCmd.getLFCmd());
            escCmd.append(escCmd.getTextCmd(textSetting, "L2"));
            escCmd.append(escCmd.getLFCmd());
            escCmd.append(escCmd.getTextCmd(textSetting, "L3: 186****8804"));
            escCmd.append(escCmd.getLFCmd());
            escCmd.append(escCmd.getTextCmd(textSetting, "L4: "));
            textSetting.setDoubleHeight(SettingEnum.Enable);
            escCmd.append(escCmd.getTextCmd(textSetting, "L5 1123"));
            escCmd.append(escCmd.getLFCmd());
            textSetting.setDoubleHeight(SettingEnum.Disable);
            textSetting.setDoubleWidth(SettingEnum.Disable);
            escCmd.append(escCmd.getTextCmd(textSetting, "------------------------------------------------"));
            escCmd.append(escCmd.getLFCmd());
            escCmd.append(escCmd.getTextCmd(textSetting, "L6                  L6                  L6"));
            escCmd.append(escCmd.getLFCmd());
            escCmd.append(escCmd.getTextCmd(textSetting, "------------------------------------------------"));
            escCmd.append(escCmd.getLFCmd());
            escCmd.append(escCmd.getTextCmd(textSetting, "L7 6415--saas  999999999999"));
            escCmd.append(escCmd.getLFCmd());
            escCmd.append(escCmd.getTextCmd(textSetting, "                      L8                   L8"));
            escCmd.append(escCmd.getLFCmd());
            escCmd.append(escCmd.getTextCmd(textSetting, "------------------------------------------------"));
            escCmd.append(escCmd.getLFCmd());
            escCmd.append(escCmd.getTextCmd(textSetting, "                                  L9:1"));
            escCmd.append(escCmd.getLFCmd());
            escCmd.append(escCmd.getLFCmd());

            escCmd.append(escCmd.getTextCmd(textSetting, "L10"));
            escCmd.append(escCmd.getLFCmd());
            escCmd.append(escCmd.getTextCmd(textSetting, "L11: !@#$%^&*() á ú "));
            escCmd.append(escCmd.getLFCmd());
            escCmd.append(escCmd.getLFCmd());

            commonSetting.setAlign(CommonEnum.ALIGN_MIDDLE);
            escCmd.append(escCmd.getCommonSettingCmd(commonSetting));

            escCmd.append(escCmd.getTextCmd(textSetting, "L12: 7FRESH"));
            escCmd.append(escCmd.getLFCmd());

            // 二维码
            barcodeSetting.setQrcodeDotSize(4);
            barcodeSetting.setQrcodeEccLevel(QrcodeEccLevel.M);
            escCmd.append(escCmd.getBarcodeCmd(BarcodeType.QR_CODE, barcodeSetting,
                    "http://weixin.qq.com/r/MSqqsnjEoiXdrTzR938j"));
            escCmd.append(escCmd.getLFCmd());

            escCmd.append(escCmd.getTextCmd(textSetting, "L13 L13 L13 L13"));
            escCmd.append(escCmd.getLFCmd());
            escCmd.append(escCmd.getTextCmd(textSetting, "L14: 1/1"));
            escCmd.append(escCmd.getLFCmd());

            // 最后额外走纸，便于撕纸
            escCmd.append(escCmd.getLFCmd());
            escCmd.append(escCmd.getLFCmd());
            escCmd.append(escCmd.getLFCmd());
            escCmd.append(escCmd.getLFCmd());
            escCmd.append(escCmd.getLFCmd());
            escCmd.append(escCmd.getLFCmd());

            // 将所有指令发送到打印机，完成打印
            rtPrinter.writeMsgAsync(escCmd.getAppendCmds());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (SdkException e) {
            callbackContext.error("EsctemplatePrint2 >> " + e.toString());
        }

    }

    private Bitmap convertBase64ToBitmap(String base64String) {
        try {
            byte[] decodedString = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        } catch (Exception e) {
            callbackContext.error("EsctemplatePrint2 >> " + e.toString());
            return null;
        }

    }
}
