package com.wcucampus.campuscompass.controller;


import static com.wcucampus.campuscompass.Constants.ERROR_DIALOG_REQUEST;
import static com.wcucampus.campuscompass.Constants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION;
import static com.wcucampus.campuscompass.Constants.PERMISSIONS_REQUEST_ENABLE_GPS;

import android.Manifest.permission;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;
import com.wcucampus.campuscompass.R;
import com.wcucampus.campuscompass.model.api.Service;
import com.wcucampus.campuscompass.model.db.CampusInfoDB;
import com.wcucampus.campuscompass.model.entity.Token;
import com.wcucampus.campuscompass.model.utility.TokenPrepper;
import com.wcucampus.campuscompass.model.utility.TokenType;
import com.wcucampus.campuscompass.view.InfoPopupFrag;
import com.wcucampus.campuscompass.view.MainMenuFragment;
import com.wcucampus.campuscompass.view.MainMenuFragment.MainMenuFragListener;
import com.wcucampus.campuscompass.view.MapsFragment;
import com.wcucampus.campuscompass.view.SearchFragAdapter.SearchFragAdapterListener;
import com.wcucampus.campuscompass.view.SearchFragment;
import com.wcucampus.campuscompass.view.SearchFragment.SearchFragListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.maps.GeoApiContext;
import com.google.maps.android.SphericalUtil;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.Retrofit.Builder;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * The main controller for the application.
 */
public class Main2Activity extends AppCompatActivity implements SearchFragListener,
    MapsFragment.MapsFragmentListener,
    InfoPopupFrag.InfoPopupFragListener, MainMenuFragListener, SearchFragAdapterListener,
    OnMapReadyCallback{

  private static final String TAG = "Main2Activity";
  private static final String BLUE_PHONE_API = "emergency.json";
  private static final String BLUE_PHONE_SOUTH_API = "emergency.json";
  private static final String BUILDING_API = "buildings.json";
  private static final String COMPUTER_POD_API = "shops.json";
  private static final String DINING_API = "lounges.json";
  private static final String HEALTHY_VENDING_API = "cafeteria.json";
  private static final String LIBRARIES_API = "libraries.json";
  private static final String PARKING_API = "ball_field.json";
  private static final String RESTROOMS_API = "Toilets.json";
  private static final String SHUTTLES_API = "buss_station.json";
  private static final int UPDATE_INTERVAL_MS = 10000;
  private static final int FASTEST_INTERVAL_MS = 5000;
  private static boolean SHOULD_FILL_DB_W_TEST = false;
  private static int NON_SEARCH_TYPES = 2;
  private static int TOTAL_TYPES = TokenType.values().length - NON_SEARCH_TYPES;

  private FragmentManager fragmentManager;
  private FrameLayout fragContainer;
  private FrameLayout mapFragContainer;
  private Toolbar toolbar;
  private int callingViewId;
  private Token targetItem;
  private InfoPopupFrag infoPopupFrag;
  private MapsFragment mapsFragment;
  private List<Token> dbTokens;
  private SearchFragment searchFragment;
  private CampusInfoDB database;
  private Retrofit retrofit;
  private boolean mLocationPermissionGranted = false;
  private FusedLocationProviderClient fusedLocationProviderClient;
  private LocationRequest mLocationRequest;
  private LocationCallback mLocationCallback;
  private Location mCurrentLocation;
  private GoogleMap myMap;
  private GeoApiContext mGeoApiContext;
  private List<String> serviceEndPoints;
  private HashMap<String, TokenType> serviceTypeMap;
  private int retries;
  private List<Marker> mapMarkers = new LinkedList<>();
  private boolean isMainFrag = true;
  private List<Token> filteredList;
  private int retriesAllowed;
  private int boundsToShowRVItems;

  /**
   * Initializes {@link android.arch.persistence.room.Database}, initializes {@link View}, initializes data, and initializes location callback.
   *
   * @param savedInstanceState contains information from a previous invocation of the class.
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main_2);
    initDB();
    initViews();
    initData();
    initLoc();
    initMapFrag();
  }

  private void initMapFrag() {
    goToMapFrag();
  }

  /**
   * Checks to make sure permission is granted for location information and GPS information.
   * If permissions are needed, begins the process to acquire permissions.
   * If permissions are granted, starts location updates.
   */
  @Override
  protected void onResume() {
    super.onResume();
    if (checkMapServices()) {
      if (mLocationPermissionGranted) {
        startLocationUpdates();
      } else {
        getLocationPermission();
      }
    }
  }

  /**
   * Inflates menu using xml resource.
   * @param menu
   * @return true
   */
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.toolbar_menu, menu);
    return true;
  }

  /**
   * Switches on {@link Toolbar} {@link MenuItem} that has been pressed.  There is just a single potential selection, which sends the user to the Main Menu.
   * @param item the item that the user selected.
   * @return boolean true if this method handled the item selected event.
   */
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_home:
        isMainFrag = true;
        swapFrags(new MainMenuFragment());
        break;
      default:
        return super.onOptionsItemSelected(item);
    }
    return true;
  }

  /**
   * Defines back button behavior. If back is pressed on home screen, exit app. Else, go to home screen.
   */
  @Override
  public void onBackPressed() {
    if(!isMainFrag){
      isMainFrag = true;
      swapFrags(new MainMenuFragment());
    }else{
      super.onBackPressed();
    }
  }


  /**
   * Initializes the {@link android.arch.persistence.room.Database}.
   */
  @Override
  protected void onStart() {
    super.onStart();
    initDB();
  }

  /**
   * Removes reference to the {@link android.arch.persistence.room.Database}.
   */
  @Override
  protected void onStop() {
    database = null;
    CampusInfoDB.forgetInstance();
    super.onStop();

  }

  /**
   * Handles tasks to do when activity is paused.
   * Stops location updates to save battery.
   */
  @Override
  protected void onPause() {
    super.onPause();
    stopLocationUpdates();
  }

  /**
   * Stops location updates.
   */
  private void stopLocationUpdates() {
    fusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
  }

  /**
   * Initializes database singleton.
   */
  private void initDB() {
    database = CampusInfoDB.getInstance(this);
  }

  /**
   * Sets views for the first time activity is accessed.
   */
  private void initViews() {
    callingViewId = R.id.building;
    fragContainer = findViewById(R.id.frag_container_2);
    mapFragContainer = findViewById(R.id.frag_container_2b);
    toolbar = findViewById(R.id.toolbar_main_2);
    toolbar.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        isMainFrag = true;
        swapFrags(new MainMenuFragment());
      }
    });
  }

  /**
   * Initializes location request loop.
   */
  private void initLoc() {
    createLocationRequest();
    mLocationCallback = new LocationCallback() {
      @Override
      public void onLocationResult(LocationResult locationResult) {
        if (locationResult == null) {
          return;
        }
        mCurrentLocation = locationResult.getLastLocation();
//        Toast.makeText(getBaseContext(), "Lat: " + mCurrentLocation.getLatitude() + "     Long: " + mCurrentLocation.getLongitude(), Toast.LENGTH_LONG).show();
        setTokenDistances();
        sortDBTokens();
      }
    };
  }
  private void initServices(){
    serviceEndPoints.add(BLUE_PHONE_API);
    serviceTypeMap.put(BLUE_PHONE_API, TokenType.BLUE_PHONE);

    serviceEndPoints.add(BLUE_PHONE_SOUTH_API);
    serviceTypeMap.put(BLUE_PHONE_SOUTH_API, TokenType.BLUE_PHONE);

    serviceEndPoints.add(BUILDING_API);
    serviceTypeMap.put(BUILDING_API, TokenType.BUILDING);

    serviceEndPoints.add(COMPUTER_POD_API);
    serviceTypeMap.put(COMPUTER_POD_API, TokenType.COMPUTER_POD);

    serviceEndPoints.add(DINING_API);
    serviceTypeMap.put(DINING_API, TokenType.DINING);

    serviceEndPoints.add(HEALTHY_VENDING_API);
    serviceTypeMap.put(HEALTHY_VENDING_API, TokenType.HEALTHY_VENDING);

    serviceEndPoints.add(LIBRARIES_API);
    serviceTypeMap.put(LIBRARIES_API, TokenType.LIBRARY);

    serviceEndPoints.add(PARKING_API);
    serviceTypeMap.put(PARKING_API, TokenType.METERED_PARKING);

    serviceEndPoints.add(RESTROOMS_API);
    serviceTypeMap.put(RESTROOMS_API, TokenType.RESTROOM);

    serviceEndPoints.add(SHUTTLES_API);
    serviceTypeMap.put(SHUTTLES_API, TokenType.SHUTTLE_STOP);


    retrofit = new Builder()
        .baseUrl(getString(R.string.unm_base_url))
        .addConverterFactory(GsonConverterFactory.create())
        .build();

  }
  private void initData() {
    // Initialize lists and maps
    dbTokens = new LinkedList<>();
    serviceEndPoints = new LinkedList<>();
    serviceTypeMap = new HashMap<>();

    // Initialize other variables
    retries = 0;
    targetItem = TokenPrepper.prep(this, new Token());
    fragmentManager = getSupportFragmentManager();
    fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

    // Retrieve last known location
    getLastKnownLocation();

    // Set toolbar as the action bar
    setSupportActionBar(toolbar);

    // Initialize services
    initServices();

    // Fill database based on configuration
    if (SHOULD_FILL_DB_W_TEST) {
      fillDBwithTest();
    } else {
      for (String endPoint : serviceEndPoints) {
        fillDBwithAPI(endPoint);
      }
    }

    // Swap fragments
    swapFrags(new MainMenuFragment());
  }

  private void startLocationUpdates() {
    if (ActivityCompat.checkSelfPermission(this, permission.ACCESS_FINE_LOCATION)
        != PackageManager.PERMISSION_GRANTED
        && ActivityCompat.checkSelfPermission(this, permission.ACCESS_COARSE_LOCATION)
        != PackageManager.PERMISSION_GRANTED) {
      return;
    }
    fusedLocationProviderClient.requestLocationUpdates(mLocationRequest,
        mLocationCallback,
        null /* Looper */);
  }

  /**
   * Creates a {@link LocationRequest} object that is stored in a field variable.
   */
  protected void createLocationRequest() {
    mLocationRequest = new LocationRequest();
    mLocationRequest.setInterval(UPDATE_INTERVAL_MS);
    mLocationRequest.setFastestInterval(FASTEST_INTERVAL_MS);
    mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
  }

  /**
   * Checks permissions, then retrieves the last known location of the device.
   */
  private void getLastKnownLocation() {
    if (ActivityCompat.checkSelfPermission(this, permission.ACCESS_FINE_LOCATION)
        != PackageManager.PERMISSION_GRANTED
        && ActivityCompat.checkSelfPermission(this, permission.ACCESS_COARSE_LOCATION)
        != PackageManager.PERMISSION_GRANTED) {
      return;
    }
    fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this,
        new OnSuccessListener<Location>() {
          @Override
          public void onSuccess(Location location) {
            if (location != null) {
              mCurrentLocation = location;
            }
          }
        });
  }
  private boolean checkMapServices() {
    if (isServicesOK()) {
      return isMapsEnabled();
    }
    return false;
  }

  /**
   * Prompts the user with a dialog to enable gps permission.
   */
  private void buildAlertMessageNoGps() {//prompts user with dialog to enable gps
    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setMessage(R.string.requires_gps)
        .setCancelable(false)
        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
          public void onClick(@SuppressWarnings("unused") final DialogInterface dialog,
              @SuppressWarnings("unused") final int id) {
            Intent enableGpsIntent = new Intent(
                android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(enableGpsIntent, PERMISSIONS_REQUEST_ENABLE_GPS);
          }
        });
    final AlertDialog alert = builder.create();
    alert.show();
  }

  /**
   * Determines whether GPS is enabled on the device.
   *
   * @return boolean true if GPS is enabled on the device.
   */
  public boolean isMapsEnabled() {//determines whether gps is enabled on the device
    final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

    if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
      buildAlertMessageNoGps();
      return false;
    }
    return true;
  }

  private void getLocationPermission() {
    /*
     * Request location permission, so that we can get the location of the
     * device. The result of the permission request is handled by a callback,
     * onRequestPermissionsResult.
     */
    if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
        android.Manifest.permission.ACCESS_FINE_LOCATION)
        == PackageManager.PERMISSION_GRANTED) {
      mLocationPermissionGranted = true;
      startLocationUpdates();
    } else {
      ActivityCompat.requestPermissions(this,
          new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
          PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
    }
  }

  /**
   * Determines whether Google Play Services can be used on the device.
   *
   * @return boolean true if the user is clear to make map requests.
   */
  public boolean isServicesOK() {//this process determines whether google play services can be used on device
    Log.d(TAG, getString(R.string.check_google_svc_version));

    int available = GoogleApiAvailability.getInstance()
        .isGooglePlayServicesAvailable(Main2Activity.this);

    if (available == ConnectionResult.SUCCESS) {
      //everything is fine and the user can make map requests
      Log.d(TAG, getString(R.string.google_play_svcs_is_working));
      return true;
    } else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
      //an error occured but we can resolve it
      Log.d(TAG, getString(R.string.google_play_svcs_error_can_fix));
      Dialog dialog = GoogleApiAvailability.getInstance()
          .getErrorDialog(Main2Activity.this, available, ERROR_DIALOG_REQUEST);
      dialog.show();
    } else {
      Toast.makeText(this, getString(R.string.cannot_make_map_requests), Toast.LENGTH_SHORT).show();
    }
    return false;
  }
  @Override
  public void onRequestPermissionsResult(int requestCode,
      @NonNull String permissions[],
      @NonNull int[] grantResults) {
    mLocationPermissionGranted = false;
    switch (requestCode) {
      case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          mLocationPermissionGranted = true;
        }
      }
    }
  }

  /**
   *  Callback from activity asking for user to enable GPS permission.  If permission is granted,
   *  then starts user location updates.
   * @param requestCode Used to ensure that this method can process the data.
   * @param resultCode Used by the call to super.
   * @param data Used by the call to super.
   */
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch (requestCode) {
      case PERMISSIONS_REQUEST_ENABLE_GPS: {
        if (mLocationPermissionGranted) {
          startLocationUpdates();
        } else {
          getLocationPermission();//asking for permission to use location services
        }
      }
    }
  }

  /**
   * Takes an end point, uses retrofit to retrieve json from end point, packages the json into a token(s), adds the token(s) to the sqlite db.
   * @param endPoint a string representing the end point
   */
  private void fillDBwithAPI(final String endPoint) {
      Service service = retrofit.create(Service.class);
      Call<List<Token>> call = service.get(endPoint);
      call.enqueue(new Callback<List<Token>>() {
        @Override
        public void onResponse(@NonNull Call<List<Token>> call,
            @NonNull Response<List<Token>> response) {
          if (!response.isSuccessful()) {
            return;
          }
          List<Token> tokensFromApi = response.body();
          if(tokensFromApi!=null){
            for (Token token :
                tokensFromApi) {
              token.setTokenType(getServiceTokenType(endPoint));
              token = TokenPrepper.prep(Main2Activity.this, token);
            }
            Token[] tokenArr = tokensFromApi.toArray(new Token[0]);
            new AddTask().execute(tokenArr);
          }
          resetRetries();
        }

        @Override
        public void onFailure(@NonNull Call<List<Token>> call, @NonNull Throwable t) {
          if(shouldStopRetrying()){
            Toast.makeText(getBaseContext(), getString(R.string.could_not_load_resource) + endPoint, Toast.LENGTH_SHORT).show();
            resetRetries();
          }else{
            fillDBwithAPI(endPoint);
          }
        }
      });
  }

  /**
   * Method to allow retries, but limit retries to 11.
   * @return true when max retries has been reached.
   */
  private boolean shouldStopRetrying() {
    retriesAllowed = 11;
    if(retries% retriesAllowed ==0){
      retries++;
      return true;
    } else {
      retries++;
      return false;
    }
  }

  /**
   * Resets retries counter.
   */
  private void resetRetries(){
    retries = 1;
  }

  /**
   * Takes an end point String and return the associated TokenType.
   * @param endPoint
   * @return TokenType
   */
  private TokenType getServiceTokenType(String endPoint) {
    if(serviceTypeMap.containsKey(endPoint)){
      return serviceTypeMap.get(endPoint);
    }
    return TokenType.BUILDING;
  }

  /**
   * A method for filling the DB with fake data.
   */
  private void fillDBwithTest() {
    Random rng = new Random();
    List<Token> prepopulateList = new LinkedList<>();
    TokenType tempType = TokenType.BUILDING;
    for (int i = 0; i < TOTAL_TYPES; i++) {
      tempType = TokenType.values()[i];
      for (int j = 0; j < rng.nextInt(25) + 25; j++) {
        Token tempToken = new Token();
        tempToken.setTokenType(tempType);
        prepopulateList.add(TokenPrepper.prep(getApplicationContext(), tempToken));
      }
    }
    Token[] tokenArr = prepopulateList.toArray(new Token[0]);
    new AddTask().execute(tokenArr);
  }

  /**
   * Swaps the passed {@link Fragment} into the fragment container, to bring it into view. Handles
   * changing the visibility of the container for the map.  Handles changing the title according to
   * the state of the app.
   *
   * @param fragIn the {@link Fragment} that will be put into the fragment container.
   */
  protected void swapFrags(Fragment fragIn) {
    if (fragIn == null) {
      return;
    }
    fragmentManager.beginTransaction()
          .replace(fragContainer.getId(), fragIn)
          .commit();
    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    if(!isMainFrag){
      mapFragContainer.setVisibility(View.VISIBLE);
      setTitle(R.string.return_to_main_menu);
    }else{
      mapFragContainer.setVisibility(View.GONE);
      setTitle(R.string.app_name);
    }
  }

  /**
   * Swaps the {@link Fragment} parameter into the fragment container, to bring it into view. Uses
   * the int parameter to set a new value for callingViewId, to keep track of which {@link View} the
   * user chose to use.
   *
   * @param fragIn the {@link Fragment} that will be put into the fragment container.
   * @param callingViewId a reference to the {@link View} the user clicked on.
   */
  protected void swapFrags(Fragment fragIn, int callingViewId) {
    this.callingViewId = callingViewId;
    if (fragIn == null) {
      return;
    }
    swapFrags(fragIn);
  }

  /**
   * Getter for callingViewId, which is the {@link View} representation of which {@link TokenType}
   * the user wants.
   *
   * @return the {@link View} that the user clicked on.
   */
  public int getCallingViewId() {
    return callingViewId;
  }

  /**
   * Getter for targetItem, which is the {@link Token} the user has requested more information for.
   * @return {@link Token} the user has most recently selected.
   */
  @Override
  public Token getTargetItem() {
    return targetItem;
  }

  /**
   * Setter for targetItem, which is the {@link Token} the user has requested more information for.
   *
   * @param theNewTarget {@link Token} the user has most recently selected.
   */
  public void setTargetItem(Token theNewTarget) {
    this.targetItem = null;
    this.targetItem = theNewTarget;
    if (infoPopupFrag != null) {
      infoPopupFrag.initData();
    }
  }

  /**
   * Returns a new SearchFragment object.
   * @return
   */
  private SearchFragment getSearchFrag() {
    searchFragment = new SearchFragment();
    return searchFragment;
  }

  /**
   * A method containing a switch that defines which data types the search fragment recyclerview should filter for.
   * @param callingViewId
   */
  private void setRVList(int callingViewId) {
    TokenType tokenType = null;

    // Determine the token type based on the calling view ID
    switch (callingViewId) {
      case R.id.iv_main_frag_0:
        tokenType = TokenType.BUILDING;
        break;
      case R.id.iv_main_frag_1:
        tokenType = TokenType.RESTROOM;
        break;
      case R.id.iv_main_frag_2:
        tokenType = TokenType.BLUE_PHONE;
        break;
      case R.id.iv_main_frag_3:
        tokenType = TokenType.COMPUTER_POD;
        break;
      case R.id.iv_main_frag_4:
        tokenType = TokenType.HEALTHY_VENDING;
        break;
      case R.id.iv_main_frag_5:
        tokenType = TokenType.DINING;
        break;
      case R.id.iv_main_frag_6:
        tokenType = TokenType.LIBRARY;
        break;
      case R.id.iv_main_frag_7:
        tokenType = TokenType.METERED_PARKING;
        break;
      case R.id.iv_main_frag_8:
        tokenType = TokenType.SHUTTLE_STOP;
        break;
      default:
        // If the calling view ID doesn't match any case, exit the method
        return;
    }

    // Execute QueryTask with the determined token type
    new QueryTask().execute(tokenType);
  }



  /**
   * Method to sort the private field dbTokens using {@link Collections} method to sort.
   */
  protected void sortDBTokens() {
    dbTokens.sort(new Comparator<Token>() {
      @Override
      public int compare(Token o1, Token o2) {
        return (Double.compare(o1.getDistance(), o2.getDistance()));
      }
    });
  }

  /**
   * Sets a reference to the most recently called {@link InfoPopupFrag} object.
   * @param infoFrag a reference to a {@link InfoPopupFrag} object.
   */
  @Override
  public void setParentRefToInfoFrag(InfoPopupFrag infoFrag) {
    this.infoPopupFrag = infoFrag;
  }

  /**
   * Sets a reference to the most recently called {@link MapsFragment}.
   * @param mapsFrag a reference to a {@link MapsFragment}.
   */
  @Override
  public void setMainRefMapsFrag(MapsFragment mapsFrag) {
    this.mapsFragment = mapsFrag;

  }

  /**
   * Calls getMapAsync() on the {@link SupportMapFragment} parameter
   * @param supportMapFragment the object that getMapAsync() will be called on.
   */
  @Override
  public void callMapAsync(SupportMapFragment supportMapFragment) {
    supportMapFragment.getMapAsync(Main2Activity.this);
  }

  /**
   * Getter for dbTokens.
   * @return {@link List} of {@link Token} objects.
   */
  @Override
  public List<Token> getTokensList() {
    return dbTokens;
  }

  /**
   * Begins method for updating markers after filtering.
   */
  @Override
  public void onSearchFiltered() {
    beginFilteredMarkerUpdate();
  }

  /**
   * Updates variable for filtered list to reference the most recent filtered list object.
   * @param filteredList the {@link List} of {@link Token} representing the filtered list.
   */
  @Override
  public void updateFilteredList(List<Token> filteredList) {
    this.filteredList = filteredList;
  }

  /**
   * Method to swap to a {@link SearchFragment}.  Uses the integer parameter to query the {@link android.arch.persistence.room.Database} for
   * the correct {@link List} of {@link Token} that will be passed to a {@link android.support.v7.widget.RecyclerView} in {@link SearchFragment}.
   * @param iD a reference to the {@link View} the user clicked on.
   */
  public void goToSearchFrag(int iD) {
    searchFragment = getSearchFrag();
    setRVList(iD);
    isMainFrag = false;
    swapFrags(searchFragment, iD);
  }

  /**
   * Method to swap to a {@link MapsFragment}. Uses the {@link Token} parameter to add a {@link Marker} to the {@link GoogleMap}.
   *
   */
  @Override
  public void goToMapFrag() {
    MapsFragment mapsFragment = new MapsFragment();
    fragmentManager.beginTransaction()
        .replace(mapFragContainer.getId(), mapsFragment)
        .commit();
    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
  }

  /**
   * Populates a {@link List} of {@link Token} objects that represent the items that are visible to the user in the {@link android.support.v7.widget.RecyclerView}.
   *
   * @param position the position of the most recently bound item in the {@link android.support.v7.widget.RecyclerView}.
   */
  @Override
  public void beginMarkerUpdate(int position){
    int first = 0;
    boundsToShowRVItems = 2;
    if(position> boundsToShowRVItems){
      first = position - boundsToShowRVItems;
    }
    List<Token> visibles = new LinkedList<>();
    List<Token> fullList = filteredList;
    int last = fullList.size() - 1;
    if(position < last - boundsToShowRVItems){
      last = position + boundsToShowRVItems;
    }
    for (int i = first; i < last; i++) {
      visibles.add(fullList.get(i));
    }

    if (visibles.size()>0) {
      updateMapMarkers(visibles);
    }
  }

  /**
   * Marks the map at the appropriate location for the user's selection.
   * @param single the {@link Token} to center the map on
   */
  @Override
  public void beginSingleMarkerUpdate(Token single){
    List<Token> visibles = new LinkedList<>();
    visibles.add(single);
    if(visibles.size()>0){
      updateMapMarkers(visibles);
    }
  }

  /**
   * Method that begins the filtered marker update process.
   */
  private void beginFilteredMarkerUpdate(){
    List<Token> visibles = new LinkedList<>();
    if (filteredList.size()>0) {
      visibles.add(filteredList.get(0));
      updateMapMarkers(visibles);
    }
  }

  /**
   * Inner class to add Tokens to the database.
   */
  private class AddTask extends AsyncTask<Token, Void, Void> {

    @Override
    protected Void doInBackground(Token... tokens) {
      List<Token> tokensList = new LinkedList<>();
      tokensList.addAll(Arrays.asList(tokens));
      database.getTokenDao().insert(tokensList);
      return null;
    }

  }

  /**
   * Inner class to retrieve Tokens from the database
   */
  private class QueryTask extends AsyncTask<TokenType, Void, List<Token>> {

    @Override
    protected List<Token> doInBackground(TokenType... types) {
      // Perform database query in the background
      return database.getTokenDao().select(types[0]);
    }

    @Override
    protected void onPostExecute(List<Token> tokens) {
      // Clear the existing data and add the new tokens
      dbTokens.clear();
      dbTokens.addAll(tokens);

      // Update distances and sort tokens
      setTokenDistances();
      sortDBTokens();

      // Update UI elements on the main thread
      updateUI();
    }

    private void updateUI() {
      // Ensure the fragment is not null before updating UI
      if (searchFragment != null && !isCancelled()) {
        // Update the adapter and redraw the list on the main thread
        searchFragment.getActivity().runOnUiThread(new Runnable() {
          @Override
          public void run() {
            searchFragment.updateListInAdapter();
            searchFragment.redrawListInAdapter();
          }
        });
      }
    }
  }


  /**
   * A method to set Token distances from user.
   */
  private void setTokenDistances() {
    for (Token token :
        dbTokens) {
      Location tempLoc = mCurrentLocation;
      LatLng myLoc = new LatLng(tempLoc.getLatitude(), tempLoc.getLongitude());
      LatLng unmToken = new LatLng(token.getMLatitude(), token.getMLongitude());
      token.setDistance((int) SphericalUtil.computeDistanceBetween(unmToken, myLoc));
    }
  }

  /**
   * Method used by {@link GoogleMap} to supply a map object.  {@link Marker} and user location is set
   * onto the map.
   * @param googleMap a map object.
   */
  @Override
  public void onMapReady(GoogleMap googleMap) {
    if(mGeoApiContext == null){
      mGeoApiContext = new GeoApiContext.Builder().apiKey(getString(R.string.google_maps_key)).build();
    }
    myMap = googleMap;
    if (ActivityCompat.checkSelfPermission(this, permission.ACCESS_FINE_LOCATION)
        != PackageManager.PERMISSION_GRANTED
        && ActivityCompat.checkSelfPermission(this, permission.ACCESS_COARSE_LOCATION)
        != PackageManager.PERMISSION_GRANTED) {
      return;
    }
    myMap.setMyLocationEnabled(true);
  }

  /**
   * A method to update the map markers and zoom the map to them.
   * @param visible
   */
  private void updateMapMarkers(List<Token> visible){
  for (Marker marker :
      mapMarkers) {
    marker.remove();
  }
  mapMarkers.clear();
  for (Token token :
      visible) {
    LatLng tempLatLng = new LatLng(token.getMLatitude(), token.getMLongitude());
    mapMarkers.add(myMap.addMarker(new MarkerOptions()
        .position(tempLatLng)
        .title(token.getTitle())
        ));
  }

  if (visible.size()>0) {
    myMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(visible.get(0).getMLatitude(), visible.get(0).getMLongitude())));
  }
  myMap.animateCamera(CameraUpdateFactory.zoomTo(18.0f));
}

  /**
   * Getter for user's current {@link Location}
   * @return current {@link Location}
   */
  @Override
  public Location getmCurrentLocation() {
    return mCurrentLocation;
  }

}
