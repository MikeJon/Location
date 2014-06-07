package com.example.map;

  

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.BMapManager;
import com.baidu.mapapi.map.Geometry;
import com.baidu.mapapi.map.Graphic;
import com.baidu.mapapi.map.GraphicsOverlay;
import com.baidu.mapapi.map.LocationData;
import com.baidu.mapapi.map.MKMapTouchListener;
import com.baidu.mapapi.map.MapController;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationOverlay;
import com.baidu.mapapi.map.OverlayItem;
import com.baidu.mapapi.map.PopupClickListener;
import com.baidu.mapapi.map.PopupOverlay;
import com.baidu.mapapi.map.RouteOverlay;
import com.baidu.mapapi.map.Symbol;
import com.baidu.mapapi.map.Symbol.Stroke;
import com.baidu.mapapi.map.TransitOverlay;
import com.baidu.mapapi.search.MKAddrInfo;
import com.baidu.mapapi.search.MKBusLineResult;
import com.baidu.mapapi.search.MKDrivingRouteResult;
import com.baidu.mapapi.search.MKPoiInfo;
import com.baidu.mapapi.search.MKPoiResult;
import com.baidu.mapapi.search.MKRoute;
import com.baidu.mapapi.search.MKSearch;
import com.baidu.mapapi.search.MKSearchListener;
import com.baidu.mapapi.search.MKShareUrlResult;
import com.baidu.mapapi.search.MKSuggestionResult;
import com.baidu.mapapi.search.MKTransitRoutePlan;
import com.baidu.mapapi.search.MKTransitRouteResult;
import com.baidu.mapapi.search.MKWalkingRouteResult;
import com.baidu.mapapi.utils.DistanceUtil;
import com.baidu.platform.comapi.basestruct.GeoPoint;
import com.example.wcsmsc.R;
import com.example.wcsmsc.SmsAplication;

public class WarnActivity extends Activity implements  BDLocationListener,MKMapTouchListener {
	// ��λ���
	LocationClient mLocClient;
	LocationData locData = null;
	//��λͼ��
	locationOverlay myLocationOverlay = null;
	//��������ͼ��
	private PopupOverlay   pop  = null;//��������ͼ�㣬����ڵ�ʱʹ��
	private TextView  popupText = null;//����view
	private View viewCache = null;
	private MapView mMapView = null;	// ��ͼView
	private MapController mMapController = null;
	boolean isRequest = false;//�Ƿ��ֶ���������λ
	boolean isFirstLoc = true;//�Ƿ��״ζ�λ
	private float lastx;
	private float lasty;
	private long  lasttime;
	private boolean islongprees = false;
	private MKSearch mSearch;
	private List<String> busLineIDList = null;
	int nodeIndex = -2;//�ڵ�����,������ڵ�ʱʹ��
	MKRoute route = null;//����ݳ�/����·�����ݵı�����������ڵ�ʱʹ��
	int busLineIndex = 0;
	private View view;
	private SeacherOperator seacherOperator ;
	private GeoPoint chosepoint;
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SmsAplication app = (SmsAplication)this.getApplication();
        if (app.mBMapManager == null) {
            app.mBMapManager = new BMapManager(getApplicationContext());
        }
        setContentView(R.layout.activity_locationoverlay);
		//��ͼ��ʼ��
        mMapView = (MapView)findViewById(R.id.bmapView);
        mMapController = mMapView.getController();
        mMapView.getController().setZoom(16);
        mMapView.getController().enableClick(true);
        mMapView.setBuiltInZoomControls(false);
        mMapView.regMapTouchListner(this);
        
        
      //���� ��������ͼ��
        createPaopao();
        
        //��λ��ʼ��
        mLocClient = new LocationClient( this );
        locData = new LocationData();
        mLocClient.registerLocationListener(this);
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true);//��gps
        option.setCoorType("bd09ll");     //������������
        option.setScanSpan(2000);
        mLocClient.setLocOption(option);
        mLocClient.start();
        mSearch = new MKSearch();
        mSearch.init(app.mBMapManager, mkSearchListener);
        //��λͼ���ʼ��
		myLocationOverlay = new locationOverlay(mMapView);
		//���ö�λ����
	    myLocationOverlay.setData(locData);
	    //��Ӷ�λͼ��
		mMapView.getOverlays().add(myLocationOverlay);
		//myLocationOverlay.enableCompass();
		//�޸Ķ�λ���ݺ�ˢ��ͼ����Ч
		mMapView.refresh();
		busLineIDList = new ArrayList<String>();
		//mSearch.poiSearchInCity("�Ϻ�", "951");
        view=findViewById(R.id.seacher);
		seacherOperator = new SeacherOperator(view);
		seacherOperator.setMkSearch(mSearch);
		seacherOperator.setOccl(occl);
		
    }
    /**
     * �ֶ�����һ�ζ�λ����
     */
    public void requestLocClick(){
    	isRequest = true;
        mLocClient.requestLocation();
        //Toast.makeText(LocationOverlayDemo.this, "���ڶ�λ����", Toast.LENGTH_SHORT).show();
    }
    /**
     * �޸�λ��ͼ��
     * @param marker
     */
    public void modifyLocationOverlayIcon(Drawable marker){
    	//������markerΪnullʱ��ʹ��Ĭ��ͼ�����
    	myLocationOverlay.setMarker(marker);
    	//�޸�ͼ�㣬��Ҫˢ��MapView��Ч
    	mMapView.refresh();
    }
    /**
	 * ������������ͼ��
	 */
	public void createPaopao(){
		viewCache = getLayoutInflater().inflate(R.layout.custom_text_view, null);
        viewCache.findViewById(R.id.settings).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent intent = new Intent(WarnActivity.this,AlarmSettings.class);
				startActivity(intent);
			}
		});
		
		popupText =(TextView) viewCache.findViewById(R.id.textcache);
        //���ݵ����Ӧ�ص�
        PopupClickListener popListener = new PopupClickListener(){
			@Override
			public void onClickedPopup(int index) {
				Log.v("click", "clickapoapo");
			}
        };
        pop = new PopupOverlay(mMapView,popListener);
       
	}
	
     
	
	
    //�̳�MyLocationOverlay��дdispatchTapʵ�ֵ������
  	public class locationOverlay extends MyLocationOverlay{

  		public locationOverlay(MapView mapView) {
  			   super(mapView);
  		}
  		@Override
  		protected boolean dispatchTap() {
  			// TODO Auto-generated method stub
  			//popupText.setBackgroundResource(R.drawable.popup);
			popupText.setText("�ҵ�λ��");
			pop.showPopup(new TextView(WarnActivity.this),new GeoPoint((int)(locData.latitude*1e6), (int)(locData.longitude*1e6)),
					8);
  			return true;
  		}
  		
  	}
  	
  	

    @Override
    protected void onPause() {
        mMapView.onPause();
        super.onPause();
    }
    
    @Override
    protected void onResume() {
        mMapView.onResume();
        super.onResume();
    }
    
    @Override
    protected void onDestroy() {
    	//�˳�ʱ���ٶ�λ
        if (mLocClient != null)
            mLocClient.stop();
        mMapView.destroy();
        super.onDestroy();
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	mMapView.onSaveInstanceState(outState);
    	
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
    	super.onRestoreInstanceState(savedInstanceState);
    	mMapView.onRestoreInstanceState(savedInstanceState);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    /**
     * ��λSDK��������
     */
	@Override
	public void onReceiveLocation(BDLocation location) {
		// TODO Auto-generated method stub
		 if (location == null)
             return ;
         locData.latitude = location.getLatitude();
         locData.longitude = location.getLongitude();
         //�������ʾ��λ����Ȧ����accuracy��ֵΪ0����
         locData.accuracy = location.getRadius();
         // �˴��������� locData�ķ�����Ϣ, �����λ SDK δ���ط�����Ϣ���û������Լ�ʵ�����̹�����ӷ�����Ϣ��
         locData.direction = location.getDerect();
         //���¶�λ����
         myLocationOverlay.setData(locData);
         //����ͼ������ִ��ˢ�º���Ч
         mMapView.refresh();
         //���ֶ�����������״ζ�λʱ���ƶ�����λ��
         if (isRequest || isFirstLoc){
         	//�ƶ���ͼ����λ��
             mMapController.animateTo(new GeoPoint((int)(locData.latitude* 1e6), (int)(locData.longitude *  1e6)));
             isRequest = false;
             //myLocationOverlay.setLocationMode(LocationMode.FOLLOWING);
         }
         //�״ζ�λ���
         isFirstLoc = false;
	}
	@Override
	public void onReceivePoi(BDLocation arg0) {
		// TODO Auto-generated method stub
		
	}
	 /*****
	  * MKMapTouchListener
	  * @param arg0
	  */
	@Override
	public void onMapClick(GeoPoint arg0) {
		// TODO Auto-generated method stub
		//
		int d =(int)DistanceUtil.getDistance(arg0, chosepoint);
		if(d<150){
			
		}else{
			pop.hidePop();
		}
	}
	@Override
	public void onMapDoubleClick(GeoPoint arg0) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onMapLongClick(GeoPoint arg0) {
		// TODO Auto-generated method stub
		int d =(int)DistanceUtil.getDistance(arg0, chosepoint);
		if(d<150){
			
		}else{
			//popupText.setBackgroundResource(R.drawable.popup);
			popupText.setText("�ҵ�λ��");
			pop.showPopup(viewCache,arg0,8);
			mMapController.animateTo(arg0);
		}
	}
	
	void SearchNextBusline(){
		 if ( busLineIndex >= busLineIDList.size()){
			 busLineIndex =0;
		 }
		 if ( busLineIndex >=0 && busLineIndex < busLineIDList.size() && busLineIDList.size() >0){
			 mSearch.busLineSearch("�Ϻ�", busLineIDList.get(busLineIndex));
			 busLineIndex ++;
		 }
		 
	}
	/******
	 * 
	 */
	
	private MKSearchListener mkSearchListener = new MKSearchListener() {
		
		@Override
		public void onGetWalkingRouteResult(MKWalkingRouteResult arg0, int arg1) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void onGetTransitRouteResult(MKTransitRouteResult arg0, int arg1) {
			// TODO Auto-generated method stub
			seacherOperator.TransitRouteResult(arg0, arg1);
		 
		 
			 
		}
		
		@Override
		public void onGetSuggestionResult(MKSuggestionResult arg0, int arg1) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void onGetShareUrlResult(MKShareUrlResult arg0, int arg1, int arg2) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void onGetPoiResult(MKPoiResult arg0, int arg1, int arg2) {
			// TODO Auto-generated method stub
			if (arg2 != 0 || arg0 == null) {
				Toast.makeText(WarnActivity.this, "��Ǹ��δ�ҵ����", Toast.LENGTH_LONG).show();
				return;
	        }
			MKPoiInfo curPoi = null;
            int totalPoiNum  = arg0.getCurrentNumPois();
            busLineIDList.clear();
            for( int idx = 0; idx < totalPoiNum; idx++ ) {
                if(2==arg0.getPoi(idx).ePoiType){
                	curPoi = arg0.getPoi(idx);
                	busLineIDList.add(curPoi.uid);
                }
			}
            SearchNextBusline();
            if(curPoi == null){
				Toast.makeText(WarnActivity.this, "��Ǹ��δ�ҵ����", Toast.LENGTH_LONG).show();
				return ;
			}
			
		}
		
		@Override
		public void onGetPoiDetailSearchResult(int arg0, int arg1) {
			// TODO Auto-generated method stub
			
			
		}
		
		@Override
		public void onGetDrivingRouteResult(MKDrivingRouteResult arg0, int arg1) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void onGetBusDetailResult(MKBusLineResult result, int arg1) {
			// TODO Auto-generated method stub
			if(result == null){
				Toast.makeText(WarnActivity.this, "��Ǹ��δ�ҵ����", Toast.LENGTH_LONG).show();
				return ;
			}
			RouteOverlay routeOverlay = new RouteOverlay(WarnActivity.this, mMapView);
		    routeOverlay.setData(result.getBusRoute());
		    mMapView.getOverlays().clear();
		    mMapView.getOverlays().add(routeOverlay);
		    mMapView.refresh();
		    mMapView.getController().animateTo(result.getBusRoute().getStart());
		    route = result.getBusRoute();
		    nodeIndex = -1;
		}
		
		@Override
		public void onGetAddrResult(MKAddrInfo arg0, int arg1) {
			// TODO Auto-generated method stub
			Log.v("WC",arg0.strAddr+"");
		}
	};
	
	private OnChildClickListener occl = new OnChildClickListener() {
		
		private int searchType;
		private WcTransitOverlay transitOverlay;

		@Override
		public boolean onChildClick(ExpandableListView parent, View v,
				int groupPosition, int childPosition, long id) {
			// TODO Auto-generated method stub
			MKTransitRoutePlan res = (MKTransitRoutePlan)v.getTag();
			 
			view.setVisibility(View.GONE);
			mMapView.setVisibility(View.VISIBLE);
			searchType = 1;
			transitOverlay = new WcTransitOverlay (WarnActivity.this, mMapView);
		    transitOverlay.setData(res);
		    mMapView.getOverlays().clear();
		    mMapView.getOverlays().add(transitOverlay);
		    mMapView.refresh();
		    mMapView.getController().zoomToSpan(transitOverlay.getLatSpanE6(), transitOverlay.getLonSpanE6());
		    mMapView.getController().animateTo(res.getStart());
		    nodeIndex = 0;
			return false;
		}
	};
	
	class WcTransitOverlay extends TransitOverlay{

		private GraphicsOverlay graphicsOverlay;

		public WcTransitOverlay(Activity arg0, MapView arg1) {
			super(arg0, arg1);
			// TODO Auto-generated constructor stub
		}

		@Override
		protected boolean onTap(int arg0) {
			// TODO Auto-generated method stub
			OverlayItem overlayItem=getItem(arg0);
			chosepoint = overlayItem.getPoint();
			popupText.setText(overlayItem.getTitle());
			pop.showPopup(viewCache,chosepoint,8);
			mMapController.animateTo(chosepoint);
			
			mMapView.getOverlays().remove(graphicsOverlay);
		    graphicsOverlay = new GraphicsOverlay(mMapView);
	        mMapView.getOverlays().add(graphicsOverlay);
	        graphicsOverlay.setData(drawCircle(chosepoint,150));
	       
			return super.onTap(arg0);
		}
		
	}
	
	 /**
     * ����Բ����Բ���ͼ״̬�仯
     * @return Բ����
     */
    public Graphic drawCircle(GeoPoint pt1,int r) {
	   	//����Բ
  		Geometry circleGeometry = new Geometry();
  	
  		//����Բ���ĵ�����Ͱ뾶
  		circleGeometry.setCircle(pt1, r);
  		//������ʽ
  		Symbol circleSymbol = new Symbol();
 		Symbol.Color circleColor = circleSymbol.new Color();
 		circleColor.red = 0;
 		circleColor.green = 255;
 		circleColor.blue = 0;
 		circleColor.alpha = 126;
  		circleSymbol.setSurface(circleColor,1,3, new Stroke(3, circleSymbol.new Color(0xFFFF0000)));
  		//����Graphic����
  		Graphic circleGraphic = new Graphic(circleGeometry, circleSymbol);
  		 
  		return circleGraphic;
   }
 
}



 
