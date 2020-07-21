package com.example.chatapp;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatapp.Adapter.MessageAdapter;
import com.example.chatapp.Model.Chat;
import com.example.chatapp.Model.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageActivity extends AppCompatActivity {

    CircleImageView profile_image;
    TextView username;

    FirebaseUser fUser;
    DatabaseReference reference;

    ImageButton btn_send,btn_img;
    EditText txt_send;

    MessageAdapter messageAdapter;
    List<Chat> mChat;

    RecyclerView recyclerView;

    Intent intent, intent2;

    //Constantes de Permisos para img's y permisos
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 200;

    private static final int IMAGE_PICK_CAMERA_CODE = 300;
    private static final int IMAGE_PICK_GALLERY_CODE = 400;

    String[] cameraPermissions;
    String[] storagePermissions;

    Uri image_rui = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        profile_image = findViewById(R.id.profile_image);
        username = findViewById(R.id.username);
        btn_send = findViewById(R.id.btn_send);
        txt_send = findViewById(R.id.text_send);


        btn_img = findViewById(R.id.btn_send_img);
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        intent = getIntent();

        final String userid = intent.getStringExtra("userid");

        //receiver id para img
        intent2 = getIntent();


        fUser = FirebaseAuth.getInstance().getCurrentUser();

        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String msg = txt_send.getText().toString();
                if(!msg.equals("")){
                    sendMessage(fUser.getUid(),userid,msg);
                }else{
                    Toast.makeText(MessageActivity.this, "No se puede enviar mensaje vacio",Toast.LENGTH_SHORT).show();
                }
                txt_send.setText("");
            }
        });

        //Boton para enviar img
        btn_img.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                showImagePickDialog();
            }
        });

        reference = FirebaseDatabase.getInstance().getReference("Users").child(userid);

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue (User.class);
                username.setText(user.getUsername());
                if (user.getImageURL().equals("default")){
                    profile_image.setImageResource(R.mipmap.ic_launcher);
                } else {
                    Glide.with(MessageActivity.this).load(user.getImageURL()).into(profile_image);
                }
                readMesagges(fUser.getUid(), userid, user.getImageURL());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void sendMessage(String sender, String receiver, String message){

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender", sender);
        hashMap.put("receiver", receiver);
        hashMap.put("message", message);
        //hashMap.put("isseen", false);
        hashMap.put("type", "text");

        reference.child("Chats").push().setValue(hashMap);
    }

    //ENVIO DE MENSAJES V-8  15.48
    private void readMesagges(final String myid, final String userid, final String imageurl){
        mChat = new ArrayList<>();

        reference = FirebaseDatabase.getInstance().getReference("Chats");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mChat.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    Chat chat = snapshot.getValue(Chat.class);
                    if (chat.getReceiver().equals(myid) && chat.getSender().equals(userid) ||
                            chat.getReceiver().equals(userid) && chat.getSender().equals(myid)){
                        mChat.add(chat);
                    }

                    messageAdapter = new MessageAdapter(MessageActivity.this, mChat, imageurl);
                    recyclerView.setAdapter(messageAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    private void sendImageMessage(Uri image_rui) throws IOException {
        //notify = true;

        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Enviando Imagen ... ");
        progressDialog.show();

        String timestamp = "" + System.currentTimeMillis();
        String fileNameAndPath = "ChatImages/"+"/post_"+timestamp;

        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),image_rui);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos );
        final byte[] data = baos.toByteArray();
        StorageReference ref = FirebaseStorage.getInstance().getReference().child(fileNameAndPath);
        ref.putBytes(data)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        progressDialog.dismiss();

                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while(!uriTask.isSuccessful());
                        String downloadUri = uriTask.getResult().toString();

                        //?
                        final String userid2 = intent2.getStringExtra("userid");

                        if(uriTask.isSuccessful()){
                            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
                            HashMap<String,Object> hashMap = new HashMap<>();
                            //fUser.getUid(),userid
                            hashMap.put("sender", fUser.getUid());
                            hashMap.put("receiver", userid2);
                            hashMap.put("message",downloadUri);
                            hashMap.put("type","image");

                            databaseReference.child("Chats").push().setValue(hashMap);

                           /*notificaimg

                            DatabaseReference databaseReference1 = FirebaseDatabase.getInstance().getReference("Users").child(senderI);
                            databaseReference1.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    User user = snapshot.getValue(User.class);

                                    if(notify){
                                        sendNo
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });

                            */
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                    }
                });
    }

    private void showImagePickDialog(){
        String[] options = {"Cámara", "Galeria"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Escoger la imagen de ...");

        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                if(which==0){
                    PickFromCamera();
                }
                if (which==1){
                    PickFromStorage();
                }
            }
        });
        builder.create().show();
    }

    private void PickFromStorage() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE);
    }

    private void PickFromCamera() {
        ContentValues cv = new ContentValues();
        cv.put(MediaStore.Images.Media.TITLE,"Temp Pick");
        cv.put(MediaStore.Images.Media.DESCRIPTION,"Temp Descrp");
        image_rui = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,cv);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, image_rui);
        startActivityForResult(intent,IMAGE_PICK_CAMERA_CODE);

    }

    //Galeria
    private boolean checkStoragePermission(){
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);
        return result;
    }

    private void requestStoragePermission (){
        ActivityCompat.requestPermissions(this,storagePermissions,STORAGE_REQUEST_CODE);
    }

    //Camara
    private boolean checkCameraPermission(){
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == (PackageManager.PERMISSION_GRANTED);

        boolean result1 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);

        return result && result1;
    }

    private void requestCameraPermission (){
        ActivityCompat.requestPermissions(this,cameraPermissions,CAMERA_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case CAMERA_REQUEST_CODE:{
                if(grantResults.length > 0){
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if(cameraAccepted && storageAccepted){
                        PickFromCamera();
                    }else{
                        Toast.makeText(this, "Permisos de Camara y Galeria son necesarios ...", Toast.LENGTH_SHORT).show();
                    }
                }else{

                }
            }
                break;
            case STORAGE_REQUEST_CODE: {
                if(grantResults.length>0){
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if(storageAccepted){
                        PickFromStorage();
                    }else
                    {
                        Toast.makeText(this,"Permisos de Galeria son necesarios", Toast.LENGTH_SHORT).show();
                    }
                }else{

                }
            }
            break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(resultCode == RESULT_OK){
            if(requestCode == IMAGE_PICK_GALLERY_CODE){
                image_rui = data.getData();
                try {
                    sendImageMessage(image_rui);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else if(requestCode == IMAGE_PICK_CAMERA_CODE){
                try {
                    sendImageMessage(image_rui);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }



}