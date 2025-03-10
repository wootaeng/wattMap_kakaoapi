package com.peng.plant.wattmap_kakaoapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;

import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.peng.plant.wattmap_kakaoapi.activity.ImageActivity;
import com.peng.plant.wattmap_kakaoapi.adapter.CustomCalloutBalloonAdapter;
import com.peng.plant.wattmap_kakaoapi.controller.TiltScrollController;
import com.peng.plant.wattmap_kakaoapi.data.ImageData;
import com.peng.plant.wattmap_kakaoapi.view.CommandView;

import net.daum.mf.map.api.CameraUpdate;
import net.daum.mf.map.api.CameraUpdateFactory;
import net.daum.mf.map.api.MapCircle;
import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapPointBounds;
import net.daum.mf.map.api.MapView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ted.gun0912.clustering.TedClustering;
import ted.gun0912.clustering.TedMap;


public class MainActivity extends AppCompatActivity implements MapView.CurrentLocationEventListener, MapView.MapViewEventListener, MapView.POIItemEventListener , TiltScrollController.ScrollListener{//

    private static final String TAG = "MainActivity";

    private Context mContext;

    //xml
    private MapView mMapView;
    private ViewGroup mMapViewContainer;

    private MapPoint mMapPoint;

    private MapPOIItem mMoveMarker;

    private Button plusBtn, minusBtn, mapUp, mapDown, mapRight, mapLeft, myLoc, watt, LocCancel, sensorStop, sensorStart,
            circle500, circle1000, circle3000, circle5000, removeC;
    private TextView zoomIn, zoomOut, map_Up, map_Down, map_Left, map_Right, locOn, locOff, wattH,
            circleVal,circle1,circle2,circle3,circle4,circleR, Loc1,Loc2,Loc3,Loc4,Loc5,LocMakDel;

    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private static final String[] REQUIRED_PERMISSIONS  = {Manifest.permission.ACCESS_FINE_LOCATION};
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;

    private double mCurrentLng, mCurrentLat, mMapX, mMapY;
    private int mZoomLevel = 0;
    private float mZoomLevelfloat = (float)0.0;

    //센서 동작
    private boolean sensor_control = false;
    private TiltScrollController mTiltScrollController;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);


        //카카오맵 view 생성
        mMapView = new MapView(this);
        mMapViewContainer = (ViewGroup) findViewById(R.id.map_view);
        mMapViewContainer.addView(mMapView);


        //기본 위치 설정
        mMapPoint = MapPoint.mapPointWithGeoCoord(37.43225475043913, 127.17844582341077);

        //GPS 못찾는다면 지도 중심점 //와트
        mMapView.setMapCenterPoint(mMapPoint,true);

        //view init
        init();

        mContext = this;

        //로컬 저장소 권한 확인
        if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);

        //GPS 확인
        if (!checkLocationServicesStatus()) {
            showDialogForLocationServiceSetting();
        }else {
            checkRunTimePermission();
        }

        //커스텀 마커 adapter 연결
        mMapView.setCalloutBalloonAdapter(new CustomCalloutBalloonAdapter(this));
        //로컬 이미지 list
        ArrayList<ImageData> list = getPathOfAllImg();
        Log.d("listsize",list.size()+"");

        //이미지list 마커list로 적용
        MapPOIItem[] poiList = new MapPOIItem[list.size()];
        //list 목록만큼 반복문 돌려 마커에 담기
        for (int i=0; i < list.size(); i++) {
            MapPOIItem item = createCustomBitmapMarker(list.get(i) , i);
            poiList[i] = item;

            int picNumber = i + 1;
            CommandView cmdView = new CommandView(this , String.format("사진 %d" , picNumber) , (R.id.id_marker_image + i));
            cmdView.setOnClickListener(pictureControl);
            cmdView.setTag(item.getItemName());
        }

        //마커 선택 반복문 -> 실패
//       for (int i=0; i < list.size(); i++){
//           int finalI = i;
//           mMapView.selectPOIItem(poiList[finalI],false);
//       }

        //map에 마커 구현
        mMapView.addPOIItems(poiList);

        //다중선택 마커
//        mMapView.selectPOIItem(poiList[1], false);

        mMapView.setMapCenterPoint(mMapPoint, false);


    }

    /**
     * 마커 관련 메소드
     * @param mapView
     * @param mapPoint
     */
    //이동 테스트 마커 메소드
    private void createMarker(MapView mapView, MapPoint mapPoint){
        mMoveMarker = new MapPOIItem();
        String name = "위치";
        mMoveMarker.setItemName(name);
        mMoveMarker.setTag(0);
        mMoveMarker.setMapPoint(mapPoint);
        mMoveMarker.setMarkerType(MapPOIItem.MarkerType.BluePin);
        mMoveMarker.setSelectedMarkerType(MapPOIItem.MarkerType.RedPin);
        mMapView.addPOIItem(mMoveMarker);
    }

    ///커스텀 마커 메소드
    private MapPOIItem createCustomBitmapMarker(ImageData data , int pos) {
        MapPOIItem m = new MapPOIItem();
        m.setItemName(data.getPath());
        m.setTag(pos);

        MapPoint point = MapPoint.mapPointWithGeoCoord(data.getLatitude(), data.getLongitude());
        m.setMapPoint(point);

        m.setMarkerType(MapPOIItem.MarkerType.CustomImage);

        File imgFile = new  File(data.getPath());
        Bitmap bm = BitmapFactory.decodeFile(imgFile.getAbsolutePath());

        m.setCustomImageBitmap(resizingBitmap(bm));
        m.setCustomImageAutoscale(false);
        m.setCustomImageAnchor(0.5f, 0.5f);
        //마커 표시 세로촬영이미지 돌아가고 가로 촬영이미지 정상 출력 문제
        //90도로 세팅시 세로 정상 가로 돌아감 // 기기가 가로모드라 그렇게 보이는거로 생각
//        m.setRotation(90f);

        return m;
    }

    //Local gallery 접근해서 이미지 목록 불러오기
    //이미지 담을 arrayList
    public ArrayList<ImageData> getPathOfAllImg() {
        ArrayList<ImageData> imageList = new ArrayList<ImageData>();

        //이미지 가져오기
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        //파일 정보 담기
        String[] projection = {
                MediaStore.MediaColumns.DATA,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.Images.ImageColumns.LATITUDE,
                MediaStore.Images.ImageColumns.LONGITUDE,
                MediaStore.MediaColumns.MIME_TYPE
        };

        //정렬하는 쿼리문
        Cursor cursor = getContentResolver().query(uri, projection, null, null, MediaStore.MediaColumns.DATE_ADDED + " desc");

        while (cursor.moveToNext()) {
            String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA));//파일경로
            String name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME));//파일이름
            Double latitude = cursor.getDouble(cursor.getColumnIndex(MediaStore.Images.ImageColumns.LATITUDE));//위도
            Double longitude = cursor.getDouble(cursor.getColumnIndex(MediaStore.Images.ImageColumns.LONGITUDE));//경도

            ImageData data = new ImageData();

            if (!TextUtils.isEmpty(path)) {
                Log.d("cursor_path : ", path);
                data.setPath(path);
            }
            if (!TextUtils.isEmpty(name)) {
                Log.d("cursor_name : ", name);
                data.setName(name);
            }

            data.setLatitude(latitude);
            data.setLongitude(longitude);

            Log.d("cursor_latitude : ", latitude.toString()+"");
            Log.d("cursor_longitude : ", longitude.toString()+"");

            imageList.add(data);
        }

        return imageList;
    }

    //사진 음성클릭 리스너
    private View.OnClickListener pictureControl = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String itemName = (String)v.getTag();
            Intent intent = new Intent(getApplicationContext(), ImageActivity.class);
            intent.putExtra("image",itemName);
            startActivity(intent);

        }
    };

    //bitmap 리사이징 메소드
    private Bitmap resizingBitmap(Bitmap bm) {
        int maxHeight = 80;
        int maxWidth = 80;
        float scale = Math.min(((float)maxHeight / bm.getWidth()), ((float)maxWidth / bm.getHeight()));
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        Bitmap bitmap = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
        return bitmap;
    }

    /****
     *
     * 마커 리스너 POIItemEventListener
     * @param mapView
     * @param mapPOIItem
     */
    @Override
    public void onPOIItemSelected(MapView mapView, MapPOIItem mapPOIItem) {
        Log.d("TAGTAGTAG", mapPOIItem.getItemName());


        Intent intent = new Intent(getApplicationContext(), ImageActivity.class);
        intent.putExtra("image",mapPOIItem.getItemName());
        startActivity(intent);
    }

    @Override
    public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem) {

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
        mMapViewContainer.removeAllViews();
    }

    /**
     * 내위치 리스너 CurrentLocationEventListener
     * @param mapView
     * @param mapPoint
     * @param v
     */
    //currentLocation 내위치 갱신
    @Override
    public void onCurrentLocationUpdate(MapView mapView, MapPoint mapPoint, float v) {
        //위치 갱신 때 값이 잘 들어오는가
        MapPoint.GeoCoordinate mapPointGeo = mapPoint.getMapPointGeoCoord();
        mMapX = mapPointGeo.latitude;
        mMapY = mapPointGeo.longitude;
        Log.d(TAG, String.format("MapView onCurrentLocationUpdate (%f,%f) accuracy (%f)", mapPointGeo.latitude, mapPointGeo.longitude, v));
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

    /**
     * 퍼미션 결과 확인 코드
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    //requestPermission 을 사용한 퍼미션 요청의 결과 리턴 메소드
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE && grantResults.length == REQUIRED_PERMISSIONS.length){
            //요청 코드가 PERMISSION_REQUEST_CODE , 요청한 퍼미션 개수만큼 수신되면
            boolean check_result = true;

            //퍼미션 허용 체크
            for (int result : grantResults){
                if (result != PackageManager.PERMISSION_GRANTED){
                    check_result = false;
                    break;
                }
            }

            if (check_result){
                //위치값 가져오기
                startTracking();
            }else {
                //퍼미션 거부시 앱 종료
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,REQUIRED_PERMISSIONS[0])){
                    Toast.makeText(MainActivity.this, "위치서비스가 거부되었습니다. 앱을 재실행하여 허용해주세요.",Toast.LENGTH_LONG).show();
                    finish();
                }else {
                    Toast.makeText(MainActivity.this, "위치서비스가 거부되었습니다. 설정에서 허용해야합니다.",Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    //런타임 퍼미션 확인
    void checkRunTimePermission(){
        //위치 퍼미션 체크
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION);

        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED){
            //퍼미션을 가지고 있다면
            //위치값 가져옴
            mMapView.setMapCenterPoint(mMapPoint,true);
        }else {//퍼미션 미허용 상태라면
            //사용자가 퍼미션 거부 한 경우
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, REQUIRED_PERMISSIONS[0])){
                Toast.makeText(MainActivity.this, "이 앱을 실행하려면 위치 접근 권한이 필요합니다.", Toast.LENGTH_LONG).show();
                //퍼미션 요청
                ActivityCompat.requestPermissions(MainActivity.this, REQUIRED_PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            }else {
                //퍼미션 거부한 적이 없는 경우 바로 요청
                ActivityCompat.requestPermissions(MainActivity.this,REQUIRED_PERMISSIONS,PERMISSIONS_REQUEST_CODE);
            }
        }
    }

    //GPS 활성화 메소드
    private void showDialogForLocationServiceSetting(){

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("위치 서비스 비활성화");
        builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n" +"위치 설정을 허용하시겠습니까?");
        builder.setCancelable(true);
        builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case GPS_ENABLE_REQUEST_CODE:
                //사용자가 GPS 활성화 시켰는지 확인
                if (checkLocationServicesStatus()){
                    if (checkLocationServicesStatus()) {
                        Log.d("@@@","onActivityResult: GPS 활성화 됨");
                        checkRunTimePermission();
                        return;
                    }
                }
                break;
        }
    }
    //GPS 확인 메소드
    private boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)||locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    //위치추적 시작
    private void startTracking(){
        mMapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading);
    }

    //위치추적 중지
    private void stopTracking(){
        mMapView.setCurrentLocationTrackingMode(MapView.CurrentLocationTrackingMode.TrackingModeOff);
    }

    /*****
     * MapviewEvent
     *
     * ****/
    @Override
    public void onMapViewInitialized(MapView mapView) {

    }

    @Override
    public void onMapViewCenterPointMoved(MapView mapView, MapPoint mapPoint) {
        //현재 지도의 중심점 좌표 확인
        MapPoint latlng= mapView.getMapCenterPoint();
        Log.d(TAG, "성공 lat+"+ latlng.getMapPointGeoCoord().latitude);
        Log.d(TAG, "성공 lng+"+ latlng.getMapPointGeoCoord().longitude);
    }
    @Override
    public void onMapViewZoomLevelChanged(MapView mapView, int i) {
        //현재 줌레벨 체크
        int zoomlevel = mapView.getZoomLevel();
        Log.d(TAG, "현재 Zoom Level : " + zoomlevel);

    }
    @Override
    public void onMapViewSingleTapped(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewDoubleTapped(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewLongPressed(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewDragStarted(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewDragEnded(MapView mapView, MapPoint mapPoint) {

    }

    @Override
    public void onMapViewMoveFinished(MapView mapView, MapPoint mapPoint) {

    }




    //확대축소 음성리스너
    private View.OnClickListener ZoomControl = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.zoomIn_text:
                    mMapView.zoomIn(true);
                    break;
                case R.id.zoomOut_text:
                    mMapView.zoomOut(true);
                    break;
            }
        }
    };

    //지도 음성 이동 리스너
    private View.OnClickListener MapMoveControl_v = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Double lat = mMapView.getMapCenterPoint().getMapPointGeoCoord().latitude;
            Double lng = mMapView.getMapCenterPoint().getMapPointGeoCoord().longitude;
            switch (v.getId()){
                case R.id.map_up:
                    mMapX = lat + 0.0035;
                    mMapY = lng;
                    break;
                case R.id.map_down:
                    mMapX = lat - 0.0035;
                    mMapY = lng;
                    break;
                case R.id.map_left:
                    mMapX = lat;
                    mMapY = lng - 0.0035;
                    break;
                case R.id.map_right:
                    mMapX = lat;
                    mMapY = lng + 0.0035;
                    break;
            }
            mMapPoint = MapPoint.mapPointWithGeoCoord(mMapX, mMapY);
            //현재 줌레벨 가져오기
            mZoomLevel = mMapView.getZoomLevel();
            //이동 좌표 update
            CameraUpdate cameraUpdate = CameraUpdateFactory.newMapPoint(mMapPoint,mZoomLevel);
            //좌표이동
            mMapView.moveCamera(cameraUpdate);
        }
    };



    //내위치 음성 리스너
    private View.OnClickListener myLocation_v = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.locOn:
                    startTracking();
                    Log.d(TAG, "위치추적 시작");
                    LocCancel.setVisibility(View.VISIBLE);
                    myLoc.setVisibility(View.GONE);
                    break;
                case R.id.locOff:
                    stopTracking();
                    Log.d(TAG,"위치추적 중지");
                    LocCancel.setVisibility(View.GONE);
                    myLoc.setVisibility(View.VISIBLE);
                    break;
                //와트 이동
                case R.id.wattH:
                    mMapPoint = MapPoint.mapPointWithGeoCoord(37.43225475043913, 127.17844582341077);
                    //현재 줌레벨 가져오기
                    mZoomLevel = mMapView.getZoomLevel();
                    //이동 좌표 update
                    CameraUpdate cameraUpdate = CameraUpdateFactory.newMapPoint(mMapPoint,mZoomLevel);
                    //좌표이동
                    mMapView.moveCamera(cameraUpdate);
                    break;
            }
        }
    };

    //화면 이동 센서 on/off 리스너
    private View.OnClickListener Sensor_control = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.sensorStop:
                    sensor_control = false;
                    sensorStop.setVisibility(View.GONE);
                    sensorStart.setVisibility(View.VISIBLE);
                    Log.d("Sensor","센서꺼짐");
                    break;
                case R.id.sensorStart:
                    sensor_control = true;
                    sensorStart.setVisibility(View.GONE);
                    sensorStop.setVisibility(View.VISIBLE);
                    break;
            }
        }
    };

    //이동 마커 테스트 리스너
    private View.OnClickListener moveLcaMarker = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            MapPoint mapPoint = null;
            CameraUpdate cameraUpdate = null;
            switch (v.getId()) {
                case R.id.loc1:
                    mapPoint = MapPoint.mapPointWithGeoCoord(37.43148342074998, 127.17725706501436);
                    createMarker(mMapView,mapPoint);
                    break;
                case R.id.loc2:
                    mapPoint = MapPoint.mapPointWithGeoCoord(37.431413210499265, 127.17269882116143);
                    createMarker(mMapView,mapPoint);
                    break;
                case R.id.loc3:
                    mapPoint = MapPoint.mapPointWithGeoCoord(37.43185622683739, 127.16886862674598);
                    createMarker(mMapView,mapPoint);
                    break;
                case R.id.loc4:
                    mapPoint = MapPoint.mapPointWithGeoCoord(37.432699654540876, 127.16558560292407);
                    createMarker(mMapView,mapPoint);
                    break;
                case R.id.loc5:
                    mapPoint = MapPoint.mapPointWithGeoCoord(37.43307808592833, 127.15858072823039);
                    createMarker(mMapView,mapPoint);
                    break;
//                case R.id.locMarkDel: //마커 삭제 코드
//
//                    break;
            }
            mZoomLevel = mMapView.getZoomLevel();
            //이동 좌표 update
            cameraUpdate = CameraUpdateFactory.newMapPoint(mapPoint,mZoomLevel);
            //좌표이동
            mMapView.moveCamera(cameraUpdate);
        }
    };




    //반경표시 음성 리스너
    private View.OnClickListener circle_voice = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.circle1:
                    customCircle(500);
                    circleVal.setText("현재 반경 : 500미터");
                    break;
                case R.id.circle2:
                    customCircle(1000);
                    circleVal.setText("현재 반경 : 1Km");
                    break;
                case R.id.circle3:
                    customCircle(3000);
                    circleVal.setText("현재 반경 : 3Km");
                    break;
                case R.id.circle4:
                    customCircle(5000);
                    circleVal.setText("현재 반경 : 5Km");
                    break;
                case R.id.circleR:
                    removeCircle();
                    circleVal.setText("");
                    break;
            }
        }
    };

    //반경 표시 메소드
    private void customCircle(int radius){
        //반경 배열
        MapCircle[] circle = mMapView.getCircles();
        if (circle.length > 0) { //새 반경시 이전 반경 삭제
            mMapView.removeCircle(circle[0]);
        }
        //현재 지도 중심점
        mMapX = mMapView.getMapCenterPoint().getMapPointGeoCoord().latitude;
        mMapY = mMapView.getMapCenterPoint().getMapPointGeoCoord().longitude;
        mMapPoint = MapPoint.mapPointWithGeoCoord(mMapX, mMapY);
        mMapView.setMapCenterPoint(mMapPoint,true);
        //반경 만들기
        MapCircle circle1 = new MapCircle(
                mMapPoint,//좌표
                radius,//범위
                Color.argb(128,255,0,0),//strokecolor테두리
                Color.argb(0,0,0,0)//fillcolor투명하게

        );
        circle1.setTag(1);
        //반경표시가 전체 나오게 하는 코드
        MapPointBounds[] mapPointBoundsArray = {circle1.getBound()};
        MapPointBounds mapPointBounds = new MapPointBounds(mapPointBoundsArray);
        int padding = 50;
        mMapView.moveCamera(CameraUpdateFactory.newMapPointBounds(mapPointBounds,padding));

        mMapView.addCircle(circle1);
    }
    //반경 제거
    private void removeCircle(){
        mMapView.removeAllCircles();
    }

    //틸트센서
    @Override
    public void onTilt(float x, float y) {
        if(sensor_control){
            //이동하는 범위
            mCurrentLat = x /1000;
            mCurrentLng = y /1000;
            //현재 중심 좌표 읽어오기
            double lat = mMapView.getMapCenterPoint().getMapPointGeoCoord().latitude;
            double lng = mMapView.getMapCenterPoint().getMapPointGeoCoord().longitude;
            mMapView.setMapCenterPoint(MapPoint.mapPointWithGeoCoord(lat - mCurrentLng , lng + mCurrentLat),false);
        }

    }

    //init View
    private void init(){

        //확대 축소 음성
        plusBtn = (Button)findViewById(R.id.zoomIn);
        minusBtn = (Button)findViewById(R.id.zoomOut);
        zoomIn = (TextView)findViewById(R.id.zoomIn_text);
        zoomOut = (TextView)findViewById(R.id.zoomOut_text);
        //지도 이동 음성버튼
        map_Up = (TextView)findViewById(R.id.map_up);
        map_Down = (TextView)findViewById(R.id.map_down);
        map_Left = (TextView)findViewById(R.id.map_left);
        map_Right = (TextView)findViewById(R.id.map_right);
        //위치음성
        locOn = (TextView)findViewById(R.id.locOn);
        locOff = (TextView)findViewById(R.id.locOff);
        wattH = (TextView)findViewById(R.id.wattH);
        watt = (Button) findViewById(R.id.watt);
        myLoc = (Button)findViewById(R.id.myLoc);
        LocCancel = (Button)findViewById(R.id.myLocCancel);
        sensorStop = (Button)findViewById(R.id.sensorStop);
        sensorStart = (Button)findViewById(R.id.sensorStart);

        circleVal = (TextView)findViewById(R.id.circleVal);
        //circle 음성
        circle1 = (TextView)findViewById(R.id.circle1);
        circle2 = (TextView)findViewById(R.id.circle2);
        circle3 = (TextView)findViewById(R.id.circle3);
        circle4 = (TextView)findViewById(R.id.circle4);
        circleR = (TextView)findViewById(R.id.circleR);
        //위치표시
        Loc1 = (TextView)findViewById(R.id.loc1);
        Loc2 = (TextView)findViewById(R.id.loc2);
        Loc3 = (TextView)findViewById(R.id.loc3);
        Loc4 = (TextView)findViewById(R.id.loc4);
        Loc5 = (TextView)findViewById(R.id.loc5);
        LocMakDel = (TextView)findViewById(R.id.locMarkDel);
        //위치표시 리스너
        Loc1.setOnClickListener(moveLcaMarker);
        Loc2.setOnClickListener(moveLcaMarker);
        Loc3.setOnClickListener(moveLcaMarker);
        Loc4.setOnClickListener(moveLcaMarker);
        Loc5.setOnClickListener(moveLcaMarker);
        LocMakDel.setOnClickListener(moveLcaMarker);
        //Map 리스너
        //지도 이동/확대/축소 이벤트
        mMapView.setMapViewEventListener(this);
        //내 위치 추적
        mMapView.setCurrentLocationEventListener(this);
        //커스텀 마커 리스너
        mMapView.setPOIItemEventListener(this);
        //줌 음성 리스너
        zoomIn.setOnClickListener(ZoomControl);
        zoomOut.setOnClickListener(ZoomControl);
        //방향 음성 리스너
        map_Up.setOnClickListener(MapMoveControl_v);
        map_Down.setOnClickListener(MapMoveControl_v);
        map_Right.setOnClickListener(MapMoveControl_v);
        map_Left.setOnClickListener(MapMoveControl_v);
        //내위치 음성 리스너
        locOn.setOnClickListener(myLocation_v);
        locOff.setOnClickListener(myLocation_v);
        //회사 음성
        wattH.setOnClickListener(myLocation_v);
        //센서 동작
        sensorStop.setOnClickListener(Sensor_control);
        sensorStart.setOnClickListener(Sensor_control);
        //반경표시 음성버튼
        circle1.setOnClickListener(circle_voice);
        circle2.setOnClickListener(circle_voice);
        circle3.setOnClickListener(circle_voice);
        circle4.setOnClickListener(circle_voice);
        circleR.setOnClickListener(circle_voice);

        mTiltScrollController = new TiltScrollController(getApplicationContext(), this);


        //개발시 참고하기 위한 버튼 view & 리스너//
        mapUp = (Button)findViewById(R.id.mapup);
        mapDown = (Button)findViewById(R.id.mapdown);
        mapRight = (Button)findViewById(R.id.mapright);
        mapLeft = (Button)findViewById(R.id.mapleft);
        //circle
        circle500 = (Button)findViewById(R.id.circle500);
        circle1000 = (Button)findViewById(R.id.circle1000);
        circle3000 = (Button)findViewById(R.id.circle3000);
        circle5000 = (Button)findViewById(R.id.circle5000);
        removeC = findViewById(R.id.removeC);
        //반경표시 버튼 리스너
        circle500.setOnClickListener(circle_control);
        circle1000.setOnClickListener(circle_control);
        circle3000.setOnClickListener(circle_control);
        circle5000.setOnClickListener(circle_control);
        removeC.setOnClickListener(circle_control);
        //내위치 리스너
        myLoc.setOnClickListener(myLocation);
        LocCancel.setOnClickListener(myLocation);
        //회사이동
        watt.setOnClickListener(myLocation);
        //방향 버튼 리스너
        mapUp.setOnClickListener(MapMoveControl);
        mapDown.setOnClickListener(MapMoveControl);
        mapRight.setOnClickListener(MapMoveControl);
        mapLeft.setOnClickListener(MapMoveControl);
        //줌 버튼 리스너
        plusBtn.setOnClickListener(ButtonZoomCon);
        minusBtn.setOnClickListener(ButtonZoomCon);

    }

    //개발 참고용 버튼 리스너
    //확대축소 버튼 리스너
    private View.OnClickListener ButtonZoomCon = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.zoomIn:
                    mMapView.zoomIn(true);
                    break;
                case R.id.zoomOut:
                    mMapView.zoomOut(true);
                    break;
            }
        }
    };

    //지도 버튼 이동 리스너
    private View.OnClickListener MapMoveControl = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Double lat = mMapView.getMapCenterPoint().getMapPointGeoCoord().latitude;
            Double lng = mMapView.getMapCenterPoint().getMapPointGeoCoord().longitude;

            switch (v.getId()){
                case R.id.mapup:
                    mMapX = lat + 0.0035;
                    mMapY = lng;
                    break;
                case R.id.mapdown:
                    mMapX = lat - 0.0035;
                    mMapY = lng;
                    break;
                case R.id.mapleft:
                    mMapX = lat;
                    mMapY = lng - 0.0035;
                    break;
                case R.id.mapright:
                    mMapX = lat;
                    mMapY = lng + 0.0035;
                    break;
            }
            mMapPoint = MapPoint.mapPointWithGeoCoord(mMapX, mMapY);
            //현재 줌레벨 가져오기
            mZoomLevel = mMapView.getZoomLevel();
            //이동 좌표 update
            CameraUpdate cameraUpdate = CameraUpdateFactory.newMapPoint(mMapPoint,mZoomLevel);
            //좌표이동
            mMapView.moveCamera(cameraUpdate);
        }
    };

    //반경표시 버튼 리스너
    private View.OnClickListener circle_control = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.circle500:
                    customCircle(500);
                    circleVal.setText("현재 반경 : 500미터");
                    break;
                case R.id.circle1000:
                    customCircle(1000);
                    circleVal.setText("현재 반경 : 1Km");
                    break;
                case R.id.circle3000:
                    customCircle(3000);
                    circleVal.setText("현재 반경 : 3Km");
                    break;
                case R.id.circle5000:
                    customCircle(5000);
                    circleVal.setText("현재 반경 : 5Km");
                    break;
                case R.id.removeC:
                    removeCircle();
                    circleVal.setText("");
                    break;
            }
        }
    };

    //내위치 버튼 리스너
    private View.OnClickListener myLocation = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.myLoc:
                    startTracking();
                    Log.d(TAG, "위치추적 시작");
                    LocCancel.setVisibility(View.VISIBLE);
                    myLoc.setVisibility(View.GONE);
                    break;
                case R.id.myLocCancel:
                    stopTracking();
                    Log.d(TAG,"위치추적 중지");
                    LocCancel.setVisibility(View.GONE);
                    myLoc.setVisibility(View.VISIBLE);
                    break;
                //watt 이동
                case R.id.watt:
                    mMapPoint = MapPoint.mapPointWithGeoCoord(37.43225475043913, 127.17844582341077);
                    //현재 줌레벨 가져오기
                    mZoomLevel = mMapView.getZoomLevel();
                    //이동 좌표 update
                    CameraUpdate cameraUpdate = CameraUpdateFactory.newMapPoint(mMapPoint,mZoomLevel);
                    //좌표이동
                    mMapView.moveCamera(cameraUpdate);
                    break;
            }
        }
    };

}