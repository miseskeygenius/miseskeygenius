package com.miseskeygenius;

/*
    Android Studio

    Disable suggestion "Anonymous [type] can be replaced with lambda" from settings:
    File -> Settings -> Editor -> Inspections
    Java -> Java language level migration aids -> Java 8

    Generate APK:
    Build -> Generate Signed APK -> Next -> release -> select V1 and V2 signatures -> Finish
    Find APK in app/release/app-release.apk
*/

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.qrcode.ErrorCorrectionLevel;
import com.qrcode.QRCode;

import static com.miseskeygenius.MisesBip32.ETHEREUM;
import static com.miseskeygenius.MisesBip32.generateSeed;
import static com.miseskeygenius.MisesBip32.getMasterPrivateKey;
import static com.miseskeygenius.MisesBip32.mnemonicsOk;
import static com.miseskeygenius.MisesBip32.seedOk;

public class MainActivity extends Activity
{
    private MisesBip32 misesBip32;

    private static Thread seedThread = null;
    private static Thread derivationThread = null;

    private int mode;
    public static final int MODE_MNEMONICS = 0;
    public static final int MODE_PASSPHRASE = 1;
    //public static final int MODE_MNEMONICS_PLUS_PASSPHRASE = 2;
    public static final int MODE_SEED = 3;
    //public static final int MODE_MPK = 4;

    Toast toast;

    // QR atributes
    int qrMaskPattern = 3;
    int maxQRsize;

    private EditTextLabel mnemonicsBox;
    private EditTextLabel passphraseBox;
    private EditTextLabel seedBox;
    private EditTextLabel mpkBox;

    private LinearLayout grayPanel;
    private SekizbitSwitch graySwitch;
    private LinearLayout grayHiddenLayout;

    private EditTextLabel graySeedBox;
    private EditTextLabel grayMpkBox;

    private EditableSpinnerLabel pathBox;
    private EditTextLabel keyNumberBox;

    private TextView keyDescriptionBox;
    private ImageView qrImageView;
    private TextView  keyBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // hide the title
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        super.onCreate(null); // savedInstanceState?
        setContentView(R.layout.activity_main);

        // find views in the XML file
        SpinnerLabel modeSpinner = findViewById(R.id.modeSpinner);
        modeSpinner.requestFocus();
        RelativeLayout graySwitchPanel = findViewById(R.id.graySwitchPanel);

        mnemonicsBox = findViewById(R.id.mnemonicsBox);
        passphraseBox = findViewById(R.id.passphraseBox);
        seedBox = findViewById(R.id.seedBox);
        mpkBox = findViewById(R.id.mpkBox);

        grayPanel = findViewById(R.id.grayPanel);
        LinearLayout graySwitchLayout = findViewById(R.id.graySwitchLayout);
        graySwitch = new SekizbitSwitch(graySwitchLayout);
        grayHiddenLayout = findViewById(R.id.grayHiddenLayout);

        graySeedBox = findViewById(R.id.graySeedBox);
        grayMpkBox = findViewById(R.id.grayMpkBox);

        SpinnerLabel coinSpinner = findViewById(R.id.coinSpinner);
        pathBox = findViewById(R.id.pathBox);
        pathBox.hideText();
        keyNumberBox = findViewById(R.id.keyNumberBox);
        SpinnerLabel ppKeySpinner = findViewById(R.id.ppKeySpinner);

        keyDescriptionBox = findViewById(R.id.keyDescriptionBox);
        qrImageView = findViewById(R.id.qrImageView);
        keyBox = findViewById(R.id.keyBox);

        // initialization
        String mnemonics = "word word word word word word";
        String seed = MisesBip32.generateSeed(mnemonics, "", MODE_MNEMONICS);
        String mpk = MisesBip32.getMasterPrivateKey(seed);
        String path = MisesBip32.paths[0][2].split(",")[0];
        String keyNumbers = "0-0";

        // fill coin spinner with coin names
        coinSpinner.setItems( new String[]{
            MisesBip32.paths[0][0],
            MisesBip32.paths[1][0],
            MisesBip32.paths[2][0]
        });

        mode = 0;
        mnemonicsBox.setText(mnemonics);
        //passphraseBox.setText("Enter your passphrase");
        seedBox.setText(seed);
        mpkBox.setText(mpk);
        graySeedBox.setText(seed);
        grayMpkBox.setText(mpk);
        pathBox.itemValues = MisesBip32.paths[0][2].split(",");
        pathBox.setText(pathBox.itemValues[0]);

        keyNumberBox.setText(keyNumbers);

        misesBip32 = new MisesBip32(mpk, path, keyNumbers);

        // set listeners for view objects
        modeSpinner.setOnItemSelectedListener(modeSpinnerListener);
        mnemonicsBox.addTextChangedListener(mnemonicsBoxWatcher);
        passphraseBox.addTextChangedListener(passphraseBoxWatcher);
        seedBox.addTextChangedListener(seedBoxWatcher);
        mpkBox.addTextChangedListener(mpkBoxWatcher);
        graySwitchPanel.setOnClickListener(graySwitchPanelListener);
        graySeedBox.addTextChangedListener(graySeedBoxWatcher);
        grayMpkBox.addTextChangedListener(grayMpkBoxWatcher);
        coinSpinner.setOnItemSelectedListener(coinSpinnerListener);
        pathBox.addTextChangedListener(pathBoxWatcher);
        keyNumberBox.addTextChangedListener(keyNumberBoxWatcher);
        ppKeySpinner.setOnItemSelectedListener(ppKeySpinnerListener);
        qrImageView.setOnClickListener(qrImageListener);
        keyBox.setOnClickListener(keyBoxListener);
        keyBox.setFocusableInTouchMode(false);

        // calculate QR max image size
        int width = Resources.getSystem().getDisplayMetrics().widthPixels;
        int height = Resources.getSystem().getDisplayMetrics().heightPixels;
        maxQRsize = width;
        if (height < width) maxQRsize = height+1-1;
        maxQRsize -= grayPanel.getPaddingLeft() * 4;
    }

    // Listener for modeSpinner
    AdapterView.OnItemSelectedListener modeSpinnerListener= new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (mode!=position) {
                modeSpinnerChanged(position);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) { }
    };

    // Listener for mnemonicsBox
    private final TextWatcher mnemonicsBoxWatcher = new TextWatcher() {

        public void afterTextChanged(Editable s) { }
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        public void onTextChanged(CharSequence mnemonics, int start, int before, int count) {
            mnemonicsBoxChanged(mnemonics.toString());
        }
    };

    // Listener for passphraseBox
    private final TextWatcher passphraseBoxWatcher = new TextWatcher() {

        public void afterTextChanged(Editable s) { }
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        public void onTextChanged(CharSequence passphrase, int start, int before, int count) {
            passphraseBoxChanged(passphrase.toString());
        }
    };

    // Listener for seedBox
    private final TextWatcher seedBoxWatcher = new TextWatcher() {

        public void afterTextChanged(Editable s) { }
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        public void onTextChanged(CharSequence seed, int start, int before, int count) {
            seedBoxChanged(seed.toString());
        }
    };

    // Listener for mpkBox
    private final TextWatcher mpkBoxWatcher = new TextWatcher() {

        public void afterTextChanged(Editable s) { }
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        public void onTextChanged(CharSequence mpk, int start, int before, int count) {
            mpkBoxChanged(mpk.toString());
        }
    };

    // Listener for graySwitchPanel
    View.OnClickListener graySwitchPanelListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            graySwitch.toggle();
            if (graySwitch.isActivated()) grayHiddenLayout.setVisibility(View.VISIBLE);
            else grayHiddenLayout.setVisibility(View.GONE);
        }
    };

    // Listener for graySeedBox
    private final TextWatcher graySeedBoxWatcher = new TextWatcher() {

        public void afterTextChanged(Editable s) { }
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        public void onTextChanged(CharSequence seed, int start, int before, int count) {
            graySeedBoxChanged(seed.toString());
        }
    };

    // Listener for grayMpkBox
    private final TextWatcher grayMpkBoxWatcher = new TextWatcher() {

        public void afterTextChanged(Editable s) { }
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        public void onTextChanged(CharSequence mpk, int start, int before, int count) {
            grayMpkBoxChanged();
        }
    };

    // Listener for coinSpinner
    AdapterView.OnItemSelectedListener coinSpinnerListener= new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            if (misesBip32.coin!=position) {
                coinSpinnerChanged(position);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) { }
    };

    // Listener for pathBox
    private final TextWatcher pathBoxWatcher = new TextWatcher() {

        public void afterTextChanged(Editable s) { }
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        public void onTextChanged(CharSequence path, int start, int before, int count) {
            pathBoxChanged(path.toString());
        }
    };

    // Listener for key number Box
    private final TextWatcher keyNumberBoxWatcher = new TextWatcher() {

        public void afterTextChanged(Editable s) { }
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        public void onTextChanged(CharSequence keyNumber, int start, int before, int count) {
            keyNumberBoxChanged(keyNumber.toString());
        }
    };

    // Listener for ppKeySpinner
    AdapterView.OnItemSelectedListener ppKeySpinnerListener= new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            // check if spinner item has been changed
            if ((position==1)!=misesBip32.isPrivateKey()) {
                ppKeySpinnerChanged(position);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) { }
    };

    View.OnClickListener qrImageListener = new View.OnClickListener() {
        public void onClick(View v) {
            qrImageClicked();
        }
    };

    View.OnClickListener keyBoxListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            keyBoxClicked();
        }
    };

    // when user changes modeSpinner...
    public void modeSpinnerChanged (int mode) {

        this.mode = mode;

        int mnemonicsVisible = View.GONE;
        int passphraseVisible = View.GONE;
        int seedVisible = View.GONE;
        int mpkVisible = View.GONE;
        int grayPanelVisible = View.VISIBLE;
        int graySeedVisible = View.VISIBLE;
        int grayMpkVisible = View.VISIBLE;

        switch(mode){
            case 0:
                // MODE_MNEMONICS
                mnemonicsVisible = View.VISIBLE;
                updategraySeed();
                break;

            case 1:
                // MODE_PASSPHRASE
                passphraseBox.setLabel("BIP32 Passphrase");
                passphraseVisible = View.VISIBLE;
                firePassphraseBoxListener();
                break;

            case 2:
                // MODE_MNEMONICS_PLUS_PASSPHRASE
                passphraseBox.setLabel("BIP39 Passphrase");
                mnemonicsVisible = View.VISIBLE;
                passphraseVisible = View.VISIBLE;
                firePassphraseBoxListener();
                break;

            case 3:
                // MODE_SEED
                seedVisible = View.VISIBLE;
                graySeedVisible = View.GONE;
                updategraySeed();
                break;

            case 4:
                // MODE_MPK
                mpkVisible = View.VISIBLE;
                grayPanelVisible = View.GONE;
                fireMpkBoxListener();
        }

        mnemonicsBox.setVisibility(mnemonicsVisible);
        passphraseBox.setVisibility(passphraseVisible);
        seedBox.setVisibility(seedVisible);
        mpkBox.setVisibility(mpkVisible);
        grayPanel.setVisibility(grayPanelVisible);
        graySeedBox.setVisibility(graySeedVisible);
        grayMpkBox.setVisibility(grayMpkVisible);
    }

    private boolean inputSeedWrong()
    {
        boolean ok = true;

        switch(mode)
        {
            case 0:
                // MODE_MNEMONICS
                if (mnemonicsBox.isWrong()) ok = false;
                break;

            case 1:
                // MODE_PASSPHRASE
                if (passphraseBox.isWrong()) ok = false;
                break;

            case 2:
                // MODE_MNEMONICS_PLUS_PASSPHRASE
                if (mnemonicsBox.isWrong()) ok = false;
                else if (passphraseBox.isWrong()) ok = false;
                break;

            case 3:
                // MODE_SEED
                if (seedBox.isWrong()) ok = false;
                break;

            case 4:
                // MODE_MPK
                if (mpkBox.isWrong()) ok = false;
        }
        return !ok;
    }

    private boolean allInputsOk(){
        boolean ok = true;

        if (inputSeedWrong()) ok = false;
        else if (pathBox.isWrong()) ok = false;
        else if (misesBip32.searchKeys & keyNumberBox.isWrong()) ok = false;

        return ok;
    }

    private void updategraySeed() {

        if (inputSeedWrong()) setStatus("ERROR", null);
        else
        {
            setStatus("PROCESSING", null);

            String mnemonics = mnemonicsBox.getText();
            String passphrase = passphraseBox.getText();
            String seed = seedBox.getText();

            if (mode==MODE_SEED) graySeedBox.setText(seed);
            else if (mode==MODE_PASSPHRASE)
                graySeedBox.setText(generateSeed(mnemonics, passphrase, mode));

            else { // MODE_MNEMONICS or MODE_MNEMONICS_PLUS_PASSPHRASE
                // kill previous thread
                if (seedThread != null) seedThread.interrupt();

                seedThread = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        // execute the hard work
                        String resultSeed = generateSeed(mnemonics, passphrase, mode);
                        boolean wasInterrupted = !carryOn();

                        runOnUiThread(new Runnable() {
                            public void run() {
                                // do post execute stuff
                                if (!wasInterrupted) graySeedBox.setText(resultSeed);
                            }
                        });
                    }
                });
                seedThread.start();
            }
        }
    }

    // update seed when mnemonics changes
    private void mnemonicsBoxChanged(String mnemonics) {

        if (mnemonicsOk(mnemonics)) {
            mnemonicsBox.setCorrect(true);
            updategraySeed();
        }
        else {
            if (derivationThread != null) derivationThread.interrupt();
            mnemonicsBox.setCorrect(false);
            setStatus("ERROR", null);
        }
    }

    private void firePassphraseBoxListener(){
        passphraseBoxChanged(passphraseBox.getText());
    }

    // update seed when passphrase changes
    private void passphraseBoxChanged(String passphrase) {

         if (passphraseOk(passphrase)) {
             passphraseBox.setCorrect(true);
             updategraySeed();
        }
        else {
             if (derivationThread != null) derivationThread.interrupt();
             passphraseBox.setCorrect(false);
             setStatus("ERROR", null);
         }
    }

    private boolean passphraseOk(String passphrase) {
        return mode!=MODE_PASSPHRASE | passphrase.length()!=0;
    }

    // update master private key when seed changes
    private void seedBoxChanged(String seed)
    {
        if (seedOk(seed)) {
            seedBox.setCorrect(true);
            graySeedBox.setText(seed);
            updategraySeed();
        }

        else {
            if (derivationThread != null) derivationThread.interrupt();
            seedBox.setCorrect(false);
            setStatus("ERROR", null);
        }
    }

    private void fireMpkBoxListener(){
        mpkBoxChanged(mpkBox.getText());
    }

    // when mpk changes ...
    private void mpkBoxChanged(String mpk){

        boolean ok = misesBip32.setMasterPrivateKey(mpk);

        if (ok) {
            mpkBox.setCorrect(true);
            setStatus("PROCESSING", null);
            grayMpkBox.setText(mpk);
        }
        else {
            if (derivationThread != null) derivationThread.interrupt();
            mpkBox.setCorrect(false);
            setStatus("ERROR", null);
        }
    }

    private void graySeedBoxChanged(String seed)
    {
        // seed has already been checked
        String mpk = getMasterPrivateKey(seed);
        misesBip32.setMasterPrivateKey(mpk);
        grayMpkBox.setText(mpk);
    }

    private void grayMpkBoxChanged(){
        // mpk has already been checked and set in misesBip32
        processDerivation();
    }

    public void coinSpinnerChanged (int coin) {
        if (coin!=misesBip32.coin) {
            misesBip32.coin=coin;

            pathBox.setItems(MisesBip32.paths[coin][1], MisesBip32.paths[coin][2]);

            // this will be called in pathBoxChanged
            //updateKeyNumberLayout();
            //processDerivation();
        }
    }
    
    // when path changes ...
    private void pathBoxChanged(String path) {

        boolean ok = misesBip32.setPath(path);

        if (ok) {
            pathBox.setCorrect(true);
            updateKeyNumberLayout();
            processDerivation();
        }
        else {
            if (derivationThread != null) derivationThread.interrupt();
            pathBox.setCorrect(false);
            setStatus("ERROR", null);
        }
    }

    /*private void updateppKeySpinner() {
        if (misesBip32.searchKeys) ppKeySpinner.setItems("Private WIF,Private Ethereum,Public Legacy,Public SegWit,Public Ethereum");
        else ppKeySpinner.setItems("Private Legacy xPrv,Public Legacy xPub,Private Segwit zPrv,Public Segwit zPub");
    }*/

    public void ppKeySpinnerChanged (int position) {
        misesBip32.setPpKey(position);

        updateKeyNumberLayout();
        processDerivation();
    }

    // when key numbers changes ...
    private void keyNumberBoxChanged(String string) {

        boolean ok = misesBip32.setKeyNumbers(string);

        if (ok) {
            keyNumberBox.setCorrect(true);
            processDerivation();
        }
        else {
            if (derivationThread != null) derivationThread.interrupt();
            keyNumberBox.setCorrect(false);
            setStatus("ERROR", null);
        }
    }
    
    private void updateKeyNumberLayout()
    {
        if (misesBip32.searchKeys) {
            // update variable for key
            String path = pathBox.getText();
            char keyLetter = path.charAt(path.length()-1);
            if (keyLetter =='\'' | keyLetter =='H') keyLetter = path.charAt(path.length()-2);

            // update max number of keys text
            int maxNKeys = misesBip32.getMaxNKeys();

            // address or key ?
            String keyType;
            if (misesBip32.isPrivateKey()) keyType = "Key";
            else  keyType = "Address";

            keyNumberBox.setVisibility(View.VISIBLE);
            keyNumberBox.setLabel(keyType + " number '" + keyLetter + "' from-to: (max " + maxNKeys + ")");
        }
        else keyNumberBox.setVisibility(View.GONE);
    }

    private void qrImageClicked(){
        // change to next mask pattern, from 0 to 7
        qrMaskPattern = (qrMaskPattern +1) % 8;
        showToast("QR Mask Pattern " + qrMaskPattern);
        qrImageView.setImageBitmap(buildQRCode());
    }

    public void keyBoxClicked()
    {
        // show toast
        int nKeys = misesBip32.getNAdresses();
        String toastText;

        if (nKeys>1) toastText = nKeys + " keys copied to clipboard";
        else toastText = "Copied to clipboard";

        showToast(toastText);
        copyToClipboard(misesBip32.keyList);
    }

    private void copyToClipboard(String text)
    {
        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("label", text);
            clipboard.setPrimaryClip(clip);
        }

        else {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            clipboard.setText(text);
        }
    }

    private void showToast(String text){
        if (toast!=null) toast.cancel();
        toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT);
        toast.show();
    }

    @SuppressLint("SetTextI18n")
    void setStatus(String status, Bitmap bitmap) {
        if (status.equals("PROCESSING")) {
            keyDescriptionBox.setText("Processing...");
            qrImageView.setVisibility(View.GONE);
            keyBox.setVisibility(View.GONE);
        }

        else if (status.equals("ERROR")) {
            keyDescriptionBox.setText("Check the required fields");
            qrImageView.setVisibility(View.GONE);
            keyBox.setVisibility(View.GONE);
        }

        else // status=="SUCCESS"
        {
            // set key description
            String pp;
            if (misesBip32.isPrivateKey()) pp = "Private";
            else pp = "Public";

            String coin;
            if (misesBip32.coin==ETHEREUM) coin = "Ethereum";
            else coin = "Bitcoin";

            int nKeys = misesBip32.getNAdresses();

            String description = "";

            if (misesBip32.searchKeys)
            {
                if (nKeys>1) description += nKeys;
                description += " " + coin + " " + pp + " ";

                if (misesBip32.isPrivateKey() & nKeys>1) description += "keys";
                else if (misesBip32.isPrivateKey()) description += "key";
                else if (nKeys>1) description += "addresses";
                else  description += "address";
            }
            else description = "Extended " + pp + " Key";

            keyDescriptionBox.setText(description);
            qrImageView.setImageBitmap(bitmap);

            if (misesBip32.searchKeys) keyBox.setText(misesBip32.keyListNumbers);
            else keyBox.setText(misesBip32.keyList);

            qrImageView.setVisibility(View.VISIBLE);
            keyBox.setVisibility(View.VISIBLE);
        }
    }

    private void processDerivation()
    {
        if (!allInputsOk() | misesBip32.inputWrong()) setStatus("ERROR", null);

        else
        {
            setStatus("PROCESSING", null);

            // kill previous thread
            if (derivationThread != null) derivationThread.interrupt();

            derivationThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    // execute the hard work
                    misesBip32.processDerivation();

                    // get the QR image
                    Bitmap bitmap = null;
                    if (carryOn()) {
                        qrMaskPattern = 3;
                        bitmap = buildQRCode();
                    }
                    final Bitmap finalBitmap = bitmap;

                    boolean wasInterrupted = !carryOn();

                    runOnUiThread(new Runnable() {
                        public void run() {
                            // do post execute stuff
                            if (!wasInterrupted) setStatus("SUCCESS", finalBitmap);
                        }
                    });
                }
            });
            derivationThread.start();
        }
    }

    private static boolean carryOn(){
        return !Thread.currentThread().isInterrupted();
    }

    public Bitmap buildQRCode()
    {
        Bitmap bitmap = null;

        int errorCorrectionLevel = ErrorCorrectionLevel.H;
        if (misesBip32.getNAdresses()>1) errorCorrectionLevel = ErrorCorrectionLevel.L;

        try {
            QRCode qrcode = new QRCode(misesBip32.keyList, errorCorrectionLevel, qrMaskPattern);

            int cellSize = maxQRsize/qrcode.getModuleCount();
            if (cellSize>10) cellSize=10;

            bitmap = qrcode.createImage(cellSize);
        } catch (Exception e) {
            System.out.println("QR error: " + e.getMessage());
        }
        return bitmap;
    }
}