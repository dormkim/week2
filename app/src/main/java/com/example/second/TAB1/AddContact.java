package com.example.second.TAB1;

import android.app.Activity;
import android.content.ContentProviderOperation;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.second.R;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public class AddContact extends AppCompatActivity {

    String newName;
    String newPhone;
    EditText nameText;
    EditText phoneText;
    ImageView userImage;
    Uri photoURI;
    Bitmap rotate_bitmap = null;

    private static final int REQUEST_TAKE_ALBUM = 222;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.addcontact_item);
        userImage = findViewById(R.id.icon);
        nameText = findViewById(R.id.name);
        phoneText = findViewById(R.id.phone);
    }

    public void onBackButtonClicked(View v) {
        Toast.makeText(getApplicationContext(), "취소했습니다.", Toast.LENGTH_SHORT).show();
        finish();
    }

    public void onAddButtonClicked(View v) {
        newName = nameText.getText().toString();
        newPhone = phoneText.getText().toString();

        if (newName.equals("")) {
            Toast.makeText(getApplicationContext(), "이름을 입력하세요.", Toast.LENGTH_SHORT).show();
        } else if (newPhone.equals("")) {
            Toast.makeText(getApplicationContext(), "번호를 입력하세요.", Toast.LENGTH_SHORT).show();
        } else {
            //연락처에 정보 넘기기
            String uri = null;
            if(photoURI != null){
                uri = photoURI.toString();
            }
            Intent intent = new Intent();
            intent.putExtra("contact_name", newName);
            intent.putExtra("contact_phone", newPhone);
            intent.putExtra("contact_uri", uri);

            addtoContacts();

            setResult(RESULT_OK, intent);
            finish();
        }
    }

    public void onPhotoButtonClicked(View v) {
        goToAlbum();
    }

    private void goToAlbum(){
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        intent.setType(android.provider.MediaStore.Images.Media.CONTENT_TYPE);
        startActivityForResult(intent, REQUEST_TAKE_ALBUM);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_TAKE_ALBUM:
                if (resultCode == Activity.RESULT_OK) {
                    if(data.getData() != null) {
                        photoURI = data.getData();
                        if (photoURI != null) {
                            // Uri - bitmap 변환
                            Bitmap bitmap = null;
                            try {
                                bitmap = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), photoURI);
                                if (bitmap != null) {
                                    String[] proj = { MediaStore.Images.Media.DATA };

                                    Cursor cursor = getContentResolver().query(photoURI, proj, null, null, null);
                                    cursor.moveToNext();
                                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA));

                                    ExifInterface exif = null;

                                    try {
                                        exif = new ExifInterface(path);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                    int exifOrientation;
                                    int exifDegree = 0;

                                    if (exif != null) {
                                        exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                                        exifDegree = exifOrientationToDegrees(exifOrientation);
                                    }
                                    rotate_bitmap = rotate(bitmap, exifDegree);
                                    userImage.setImageBitmap(rotate_bitmap);
                                }
                            } catch (FileNotFoundException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    }
                }
                break;
        }
    }

    /*전화번호부 로컬에 저장하기*/
    public void addtoContacts(){
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                .build());

        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, newName)
                .build());

        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, newPhone)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .build());


        if(rotate_bitmap != null) {
            //byte[]어레이 변환
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            rotate_bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
            byte[] bytes = stream.toByteArray();

            //사진 추가
            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, bytes)
                    .build());
        }

        try {
            getApplicationContext().getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        } catch (Exception e) {
            Log.e("ContactsAdder", "Exceptoin encoutered while inserting contact: " + e);
        }
    }

    private int exifOrientationToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270;
        }
        return 0;
    }

    private Bitmap rotate(Bitmap bitmap, float degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }
}
