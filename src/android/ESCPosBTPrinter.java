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
            callbackContext.error("printBarCode >> " + e.toString());
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
            callbackContext.error("applyBarcodeSettings >> " + e.toString());
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
            if (config.has("chartSetName")) {
                escCmd.setChartsetName(config.getString("chartSetName"));
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

            /*
             * Bitmap bitLogo = convertBase64ToBitmap(
             * "";
             * if (bitLogo != null) {
             * BitmapSetting bitmapSetting = new BitmapSetting();
             * bitmapSetting.setBimtapLimitWidth(208);
             * bitmapSetting.setBmpPrintMode(BmpPrintMode.MODE_SINGLE_COLOR);
             * escCmd.append(escCmd.getBitmapCmd(bitmapSetting, bitLogo));
             * }
             */

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
