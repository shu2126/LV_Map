package com.example.lv_map;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapReverseGeoCoder;
import net.daum.mf.map.api.MapView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements MapView.POIItemEventListener,MapView.CurrentLocationEventListener,
        MapReverseGeoCoder.ReverseGeoCodingResultListener, TextWatcher {//, TextWatcher

    private ArrayList<ListData> myArrayList;

    static MapView myMap;
    static String string;

    private ViewGroup mapViewContainer;

    private Button typeVisiblity_btn, standard_btn, satellite_btn,
            hybrid_btn, choose_btn,navigation_btn, foot, car, bus;
    private TableLayout typeTable, naviTable;
    private EditText searchKeyword;
    private RecyclerView listView;

    //====================================================================
    private ListAdapter myAdapter;
    //====================================================================
    private static MapPOIItem marker;

    // 위도 경도 최종 위치
    private double myLongitude, myLatitude,lat=0.0, lon=0.0;
    private String mJsonString, url;
    private int nabiNum=0;

    private static boolean isTrackingMode;
    //=====================================================================
    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    String[] REQUIRED_PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //============필요한 객체 선언 및 레이아웃 연결

        string = "addr";
        typeVisiblity_btn = (Button) findViewById(R.id.typeVisiblity_btn); //지도 타입 테이블 visiblity속성 버튼
        satellite_btn = (Button) findViewById(R.id.satellite);              //satellite 모드 전환 버튼
        standard_btn = (Button) findViewById(R.id.standard);                //standard 모드 전환 버튼
        hybrid_btn = (Button) findViewById(R.id.hybrid);                    //hybrid 모드 전환 버튼
        typeTable = (TableLayout) findViewById(R.id.typeTable);             // 지도 타입 버튼
        choose_btn = (Button) findViewById(R.id.choose_btn);                // 검색 키워드 선택

        searchKeyword = (EditText) findViewById(R.id.searchKeyword);
        navigation_btn = (Button) findViewById(R.id.navigation_btn);
        naviTable = (TableLayout)findViewById(R.id.naviTable);
        foot = (Button)findViewById(R.id.foot);
        car = (Button)findViewById(R.id.car);
        bus =(Button)findViewById(R.id.bus);

    //==============================================================================
        //화면에 지도띄우기
        mapViewContainer = (ViewGroup) findViewById(R.id.mapView);
        myMap = new MapView(this);
        mapViewContainer.addView(myMap);//지도 띄우기
        // 현재 위치 정보를 위한 객체 myMap(MapView)등록
        myMap.setCurrentLocationEventListener(this);
        //초기 지도 줌 레벨 설정
        myMap.setZoomLevel(6,true);

        //줌 인아웃할때 애니메이션 효과 true
        myMap.zoomOut(true);
        myMap.zoomIn(true);
        //지도 위에 있는 마커나 말풍선 등을 눌렀을때 이벤트 구현
        myMap.setPOIItemEventListener((MapView.POIItemEventListener) this);
        // 현위치 트래킹 모드 설정(나침반모드 설정)
        myMap.setCurrentLocationTrackingMode(
                MapView.CurrentLocationTrackingMode.TrackingModeOnWithHeading);
//==============================================================================
        listView = (RecyclerView) findViewById(R.id.listView);
        listView.setLayoutManager(new LinearLayoutManager(this));
        myArrayList = new ArrayList<>();
        myAdapter = new ListAdapter(this,myArrayList);
        listView.setAdapter(myAdapter);

//==============================================================================
        if (!checkLocationServicesStatus()) {
            showDialogForLocationServiceSetting();
        }else {
            checkRunTimePermission();
        }

        //서버 연결
        InsertData task = new InsertData();
        task.execute("http://192.168.0.14/android.php");

        searchKeyword.addTextChangedListener(this);
        ButtonClickListener();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence search, int start, int before, int count) {

        myAdapter.getFilter().filter(search);
    }

    //"http://192.168.0.14/android.php"
    //php 소켓 통신
    class InsertData extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
        @Override
        protected void onPostExecute(String result){
            if(result==null){
                Toast.makeText(getApplicationContext(),
                        "서버에서 정보를 가져오지 못했습니다.", Toast.LENGTH_LONG).show();
            }
            else{
                mJsonString = result;
                showResult();
                Toast.makeText(getApplicationContext(),
                        "서버에서 정보를\n가져왔습니다.", Toast.LENGTH_LONG).show();
            }
        }
        @SuppressLint("SetTextI18n")
        @Override
        protected String doInBackground(String... params) {
            try {
                String name = (String)"hello!!";
                String postParameters = "name="+name;
                URL url = new URL(params[0]);
                HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();


                httpURLConnection.setReadTimeout(3000);
                httpURLConnection.setConnectTimeout(3000);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.connect();

                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(postParameters.getBytes("UTF-8"));
                outputStream.flush();
                outputStream.close();

                InputStream inputStream;

                int responseStatusCode = httpURLConnection.getResponseCode();
                Log.d("POST_RESPONSE_2", "test : " + responseStatusCode);


                if(responseStatusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = httpURLConnection.getInputStream();
                    Log.d("CONN", "성공");
                }
                else {
                    inputStream = httpURLConnection.getErrorStream();
                    Log.d("CONN","실패");
                }

                InputStreamReader inputStreamReader = new InputStreamReader(inputStream,"UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuilder sb = new StringBuilder();
                String line;

                while((line=bufferedReader.readLine()) != null){
                    sb.append(line);
                }
                bufferedReader.close();
                return sb.toString().trim();

            } catch (Exception e) {
                e.printStackTrace();
                Log.e("ExceptionError","서버 접속 실패 Error : "+e);
                return null;
            }
        }
    }

    private void showResult() {

        String TAG_JSON = "result";
        String TAG_NAME = "name";
        String TAG_DETAILADDR = "detailaddr";
        String TAG_TYPE = "type";
        String TAG_NUMBER = "number";
        String TAG_ADDR = "addr";

        try {
            //소켓통신후 return받은 값을 json형으로 변환
            //php에서 보인 json형의 괄호를 제외하고 넣음
            JSONObject jsonObject = new JSONObject(mJsonString.substring(
                    mJsonString.indexOf("{"),mJsonString.lastIndexOf("}") +1));
            //json 정보 json배열에 넣음
            JSONArray jsonArray = jsonObject.getJSONArray(TAG_JSON);

            for (int i = 0; i < jsonArray.length(); i++) {

                JSONObject item = jsonArray.getJSONObject(i);

                //태그(id값이라고 생각)는 각 데이터가 알맞은 자리에 들어가기위함
                String name = item.getString(TAG_NAME);
                String type = item.getString(TAG_TYPE);
                String detailaddr = item.getString(TAG_DETAILADDR);
                String number = item.getString(TAG_NUMBER);
                String addr = item.getString(TAG_ADDR);

                //recyclerview의 item객체는 ListData
                ListData listData = new ListData();

                //recyclerview에 데이터 넣기
                listData.setName(name);
                listData.setType(type);
                listData.setDetailaddr(detailaddr);
                listData.setNumber(number);
                listData.setAddr(addr);

                myArrayList.add(listData); //listData 클래스
                myAdapter.notifyDataSetChanged(); // 데이터 추가 후 myAdapter 새로고침
            }
        } catch (JSONException e) {
            Log.d("JSONExceptionString", "showResult : ", e);
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    public void ButtonClickListener() {

        typeVisiblity_btn.setOnClickListener(view -> {
            if (typeTable.getVisibility() == View.VISIBLE)
                typeTable.setVisibility(View.INVISIBLE);
            else typeTable.setVisibility(View.VISIBLE);
        });

        //지도 유형 기본
        standard_btn.setOnClickListener(view -> {
            myMap.setMapType(MapView.MapType.Standard);
            typeVisiblity_btn.setText(standard_btn.getText());
            typeTable.setVisibility(View.INVISIBLE);
        });

        //지도 유형 유성지도
        satellite_btn.setOnClickListener(view -> {
            myMap.setMapType(MapView.MapType.Satellite);
            typeVisiblity_btn.setText(satellite_btn.getText());
            typeTable.setVisibility(View.INVISIBLE);
        });

        //지도 유형 하이브리드
        hybrid_btn.setOnClickListener(view -> {
            myMap.setMapType(MapView.MapType.Hybrid);
            typeVisiblity_btn.setText(hybrid_btn.getText());
            typeTable.setVisibility(View.INVISIBLE);
        });

        //지역 or 상표 검색 버튼
        choose_btn.setOnClickListener(view -> {

            searchKeyword.setText("");
            if (choose_btn.getText().equals("행정동")) {
                choose_btn.setText("상호");
                searchKeyword.setHint("검색할 상호명을 입력해주세요");
                string = "name";
            }
            else if (choose_btn.getText().equals("상호")){
                choose_btn.setText("업종");
                searchKeyword.setHint("검색할 업종을 입력해주세요");
                string = "type";
            }
            else if (choose_btn.getText().equals("업종")){
                choose_btn.setText("행정동");
                searchKeyword.setHint("검색할 행정동을 입력해주세요");
                string = "addr";
            }
        });

        navigation_btn.setOnClickListener(view->{
            if(naviTable.getVisibility()==View.VISIBLE){
                naviTable.setVisibility(View.INVISIBLE);
            }
            else naviTable.setVisibility(View.VISIBLE);
        });

        foot.setOnClickListener(view->{
            naviTable.setVisibility(View.INVISIBLE);

            if(ListAdapter.testStr == null){
                Toast.makeText(getApplicationContext(), "위치 지정할 곳을 선택해 주세요.", Toast.LENGTH_LONG).show();
            }
            else{
                OpenNavi(navigation_btn, foot.getText().toString(),
                        myLatitude+","+myLongitude+"&ep="
                                +ListAdapter.latitude+","+ListAdapter.longitude+"&by=FOOT" );
            }
        });
        car.setOnClickListener(view->{
            naviTable.setVisibility(View.INVISIBLE);

            if(ListAdapter.testStr == null ){
                Toast.makeText(getApplicationContext(), "위치 지정할 곳을 선택해 주세요.", Toast.LENGTH_LONG).show();
            }
            else{
                OpenNavi(navigation_btn, car.getText().toString(),
                        myLatitude+","+myLongitude+"&ep="
                                +ListAdapter.latitude+","+ListAdapter.longitude+"&by=CAR" );
            }
        });
        bus.setOnClickListener(view->{
            naviTable.setVisibility(View.INVISIBLE);

            if(ListAdapter.testStr == null){
                Toast.makeText(getApplicationContext(), "리스트에서 선택해주세요", Toast.LENGTH_LONG).show();
            }
            else{
                OpenNavi(navigation_btn, bus.getText().toString(),
                        myLatitude+","+myLongitude+"&ep="
                                + ListAdapter.latitude+","+ListAdapter.longitude+"&by=PUBLICTRANSIT" );
//                kakaomap://route?sp=37.537229,127.005515&ep=37.4979502,127.0276368&by=FOOT
            }
        });
        searchKeyword.setOnKeyListener(new View.OnKeyListener(){
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(keyCode==event.KEYCODE_ENTER) return true;
                return false;
            }
        });

    }

    // kakaomap://route?sp=도착 지점 위도,도착 지점 경도&ep=출발 지점 위도,출발 지점 경도&by=FOOT 도보길찾기
    //FOOT = 도보길찾기, CAR = 자가용 길찾기, PUBLICTRANSIT = 대중교통 길찾기
    public void OpenNavi(Button button, String getText, String str){
        Intent intent;
        url = "kakaomap://route?sp="+str;
        try {
            Toast.makeText(getApplicationContext(), "카카오맵으로 길찾기를 시도합니다.", Toast.LENGTH_SHORT).show();
            //카카오맵 앱 실행
            intent = new Intent(Intent.ACTION_VIEW,Uri.parse(url));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        catch (Exception e) {
            Toast.makeText(getApplicationContext(), "길찾기에는 카카오맵이 필요합니다." +
                    " 다운받아주시길 바랍니다.", Toast.LENGTH_SHORT).show();
            //카카오맵 앱 다운받기
            intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(
                    "https://play.google.com/store/apps/details?id=net.daum.android.map&hl=ko"));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        button.setText(getText);
    }

    public void setDaumMapCurrentMarker(Double latitude, Double longitude, String name) {
        marker = new MapPOIItem();

        isTrackingMode = false;
        marker.setItemName(name); //마커 클릭시 보이는 풍선에 넣을 값
        marker.setTag(0);
        marker.setMapPoint(MapPoint.mapPointWithGeoCoord(latitude, longitude));
        marker.setMarkerType(MapPOIItem.MarkerType.BluePin); // 파란 마커 모양
        marker.setSelectedMarkerType(MapPOIItem.MarkerType.RedPin); // 마커 클릭시 빨간색으로
        marker.setCustomImageAnchor(0.5f, 1.0f);
        // 마커 이미지중 기준이 되는 위치(앵커포인트) 지정
        // 마커 이미지 좌측 상단 기준 x(0.0f ~ 1.0f), y(0.0f ~ 1.0f) 값.
        //마커 지도에 추가(표시)
        myMap.addPOIItem(marker);
    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    @Override        //지도위의 POIItem(마커)클릭시 이벤트
    public void onPOIItemSelected(MapView mapView, MapPOIItem mapPOIItem) {

        int target = mapPOIItem.getItemName().indexOf("-"); //말풍선 속 상호명 추출
        Toast.makeText(MainActivity.this,
                mapPOIItem.getItemName().substring(0,target)+" 클릭", Toast.LENGTH_SHORT).show();

        //필터링을 위한 static String
        //어댑터에서 string보고 어떤거 필터링해서 보여줄지 결정
        string = "name";
        ListAdapter.breakNum = 1; // 찍히고 있던 마커가 있다면 stop
        //검색창 검색시 필터링을 위한 문자 실시간 인식
        searchKeyword.addTextChangedListener(this);
        //검색창에 상호명을 넣어 리스트에 표시
        searchKeyword.setText(mapPOIItem.getItemName().substring(0,target));
        choose_btn.setText("상호");
        ListAdapter.breakNum = 0; //마커표시
    }

    @Override           //마커 클릭시 나타나는 말풍선표시
    public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem) {
        Toast.makeText(MainActivity.this, "클릭", Toast.LENGTH_SHORT).show();

        int target = mapPOIItem.getItemName().indexOf("-");
        String str = mapPOIItem.getItemName().substring(0,target);

        //네이버로 검색할건지 안내창띄우기
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(mapPOIItem.getItemName().substring(0,target)+" 정보를 보시겠습니까?");

        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
            @Override //예 클릭시 이벤트
            public void onClick(DialogInterface dialogInterface, int i) {
                //(Info.class)웹뷰 보여주는 클래스, 웹뷰로 화면 전환
                Intent intent = new Intent(getApplicationContext(),Info.class);
                intent.putExtra("name",str);//전환하면서 상호명 전달
                startActivity(intent);
            }
        });
        builder.setNegativeButton("취소", null); //안내창 끄기
        builder.create().show(); //안내창 생성
    }

    @Override
    public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem, MapPOIItem.CalloutBalloonButtonType calloutBalloonButtonType) {

    }

    @Override
    public void onDraggablePOIItemMoved(MapView mapView, MapPOIItem mapPOIItem, MapPoint mapPoint) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onCurrentLocationUpdate(MapView mapView, MapPoint currentLocation, float accuracyInMeters) {
        MapPoint.GeoCoordinate mapPointGeo = currentLocation.getMapPointGeoCoord();
        myLatitude = mapPointGeo.latitude;
        myLongitude = mapPointGeo.longitude;

//        Toast.makeText(getApplicationContext(),"1 . lat"+mapPointGeo.latitude+"long : "+ mapPointGeo.longitude, Toast.LENGTH_LONG).show();
    }


    @Override
    public void onCurrentLocationDeviceHeadingUpdate(MapView mapView, float v) {

    }

    @Override
    public void onCurrentLocationUpdateFailed(MapView mapView) {

    }

    @Override
    public void onCurrentLocationUpdateCancelled(MapView mapView) {

    }

    @Override
    public void onReverseGeoCoderFoundAddress(MapReverseGeoCoder mapReverseGeoCoder, String s) {
        mapReverseGeoCoder.toString();
        onFinishReverseGeoCoding(s);
    }

    @Override
    public void onReverseGeoCoderFailedToFindAddress(MapReverseGeoCoder mapReverseGeoCoder) {
        onFinishReverseGeoCoding("Fail");
    }

    private void onFinishReverseGeoCoding(String result) {
//        Toast.makeText(LocationDemoActivity.this, "Reverse Geo-coding : " + result, Toast.LENGTH_SHORT).show();
    }
    /*
     * ActivityCompat.requestPermissions를 사용한 퍼미션 요청의 결과를 리턴받는 메소드입니다.
     */
    @Override
    public void onRequestPermissionsResult(int permsRequestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grandResults) {

        if ( permsRequestCode == PERMISSIONS_REQUEST_CODE && grandResults.length == REQUIRED_PERMISSIONS.length) {

            // 요청 코드가 PERMISSIONS_REQUEST_CODE 이고, 요청한 퍼미션 개수만큼 수신되었다면

            boolean check_result = true;


            // 모든 퍼미션을 허용했는지 체크합니다.

            for (int result : grandResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false;
                    break;
                }
            }


            if ( check_result ) {
                Log.d("@@@", "start");
                //위치 값을 가져올 수 있음

            }
            else {
                // 거부한 퍼미션이 있다면 앱을 사용할 수 없는 이유를 설명해주고 앱을 종료합니다.2 가지 경우가 있습니다.

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])) {

                    Toast.makeText(MainActivity.this, "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요.", Toast.LENGTH_LONG).show();
                    finish();


                }else {

                    Toast.makeText(MainActivity.this, "퍼미션이 거부되었습니다. 설정(앱 정보)에서 퍼미션을 허용해야 합니다. ", Toast.LENGTH_LONG).show();

                }
            }

        }
    }

    void checkRunTimePermission(){

        //런타임 퍼미션 처리
        // 1. 위치 퍼미션을 가지고 있는지 체크합니다.
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION);


        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED ) {

            // 2. 이미 퍼미션을 가지고 있다면
            // ( 안드로이드 6.0 이하 버전은 런타임 퍼미션이 필요없기 때문에 이미 허용된 걸로 인식합니다.)


            // 3.  위치 값을 가져올 수 있음
//            myMap.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithHeading);


        } else {  //2. 퍼미션 요청을 허용한 적이 없다면 퍼미션 요청이 필요합니다. 2가지 경우(3-1, 4-1)가 있습니다.

            // 3-1. 사용자가 퍼미션 거부를 한 적이 있는 경우에는
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, REQUIRED_PERMISSIONS[0])) {

                // 3-2. 요청을 진행하기 전에 사용자가에게 퍼미션이 필요한 이유를 설명해줄 필요가 있습니다.
                Toast.makeText(MainActivity.this, "이 앱을 실행하려면 위치 접근 권한이 필요합니다.", Toast.LENGTH_LONG).show();
                // 3-3. 사용자게에 퍼미션 요청을 합니다. 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);


            } else {
                // 4-1. 사용자가 퍼미션 거부를 한 적이 없는 경우에는 퍼미션 요청을 바로 합니다.
                // 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);
            }

        }

    }

    //여기부터는 GPS 활성화를 위한 메소드들
    private void showDialogForLocationServiceSetting() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("위치 서비스 비활성화");
        builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n"
                + "위치 설정을 수정하실래요?");
        builder.setCancelable(true);
        builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent
                        = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case GPS_ENABLE_REQUEST_CODE:

                //사용자가 GPS 활성 시켰는지 검사
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {

                        Log.d("@@@", "onActivityResult : GPS 활성화 되있음");
                        checkRunTimePermission();
                        return;
                    }
                }

                break;
        }
    }

    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

    }
}

