package com.example.chatapp.Fragments;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.chatapp.Model.User;
import com.example.chatapp.R;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
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
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import org.w3c.dom.Document;

import java.util.HashMap;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.app.Activity.RESULT_OK;


public class ProfileFragment extends Fragment {
    CircleImageView image_profile;
    TextView username;
    DatabaseReference reference;
    FirebaseUser fuser;
    /*//agregado
    FirebaseAuth fAuth;
    FirebaseStorage fStorage;
    String userId;
    //*/
    StorageReference storageReference;
    private static final int IMAGE_REQUEST=1;

    //private static final int IMAGE_PICK_GALLERY_CODE = 400;
    private Uri imageUri;
    private StorageTask<UploadTask.TaskSnapshot> uploadTask;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =inflater.inflate(R.layout.fragment_profile,container, false);

        image_profile=view.findViewById(R.id.profile_image);
        username=view.findViewById(R.id.username);


        storageReference= FirebaseStorage.getInstance().getReference("uploads");
        /*//agregado
        fAuth=FirebaseAuth.getInstance();
        fStorage=FirebaseStorage.getInstance();
        userId=fAuth.getCurrentUser().getUid();
        //*/

        fuser= FirebaseAuth.getInstance().getCurrentUser();
        reference= FirebaseDatabase.getInstance().getReference("Users").child(fuser.getUid());

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if(isAdded()){
                    //Load image if the fragment is currently added to its activity.
                    User user = dataSnapshot.getValue(User.class);
                    username.setText(user.getUsername());
                    if (user.getImageURL().equals("default")) {
                        image_profile.setImageResource(R.mipmap.ic_launcher);

                    }else{
                        Glide.with(getContext()).load(user.getImageURL()).into(image_profile);
                    }
                }



            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
///Subir imagen PARA PERFIL DE usuario
        image_profile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openImage();
            }
        });

        return view;
    }
//METODO PARA ABRIR IMAGENES  DEL STORAGE
    private void openImage() {
        Intent intent=new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, IMAGE_REQUEST);

    }

    private String getFileExtension(Uri uri){
        ContentResolver contentResolver=getContext(). getContentResolver();
        MimeTypeMap mimeTypeMap=MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));

    }

    private void uploadImage(){
        final ProgressDialog pd= new ProgressDialog(getContext());
        pd.setMessage("Uploading");
        pd.show();
        if (imageUri !=null) {
            final StorageReference fileReferences=storageReference.child(System.currentTimeMillis()
                    +"."+getFileExtension(imageUri));

            uploadTask=fileReferences.putFile(imageUri);
            uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return  fileReferences.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Uri downloadUri=task.getResult();
                        String mUri=downloadUri.toString();

                        reference=FirebaseDatabase.getInstance().getReference("Users").child(fuser.getUid());
                        HashMap<String ,Object> map=new HashMap<>();
                        map.put("imageURL", mUri);
                        reference.updateChildren(map);

                        pd.dismiss();
                    }else{
                        Toast.makeText(getContext(),"Falló" ,Toast.LENGTH_SHORT).show();
                        pd.dismiss();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(getContext(),e.getMessage(),Toast.LENGTH_SHORT).show();
                    pd.dismiss();
                }
            });
        }else{
            Toast.makeText(getContext(), "No seleccionó ninguna imagen",Toast.LENGTH_SHORT).show();
        }
    }
    public void onActivityResult(int requestCode,int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode,data);
        if (requestCode==IMAGE_REQUEST && resultCode==RESULT_OK && data.getData()!=null) {
            imageUri=data.getData();

            if(uploadTask!=null && uploadTask.isInProgress()){
                Toast.makeText(getContext(), "subiendo...", Toast.LENGTH_SHORT).show();
            }else{
                uploadImage();
            }
        }
    }
}