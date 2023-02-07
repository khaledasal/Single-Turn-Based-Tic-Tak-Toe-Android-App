package com.example.tictactoe;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class PlayerName extends AppCompatActivity
{
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player_name);
    
        final EditText playerNameEt = findViewById(R.id.playerNamerEt);
        final AppCompatButton startGameBtn = findViewById(R.id.startGameBtn);
        final MediaPlayer start_chime;
    
        start_chime = MediaPlayer.create(this, R.raw.start_bell);
        
        startGameBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                final int playerScore = 0;
                final int opponentScore = 0;
                
                // getting playername from EditText to a String variable
                final String getPlayerName = playerNameEt.getText().toString();
            
                // checking whether player has entered his name
                if (getPlayerName.isEmpty()){
                    Toast.makeText(PlayerName.this, "Please enter player name", Toast.LENGTH_SHORT).show();
                }
                else{
                
                    // creating intent to open MainActivity
                    Intent intent = new Intent(PlayerName.this, MainActivity.class);
                
                    // adding player name along with intent
                    intent.putExtra("playerName",getPlayerName);
                    intent.putExtra("rematch",false);
                    intent.putExtra("playerScore",playerScore);
                    intent.putExtra("opponentScore",opponentScore);
                    // opening MainActivity
                    startActivity(intent);
                
                    // destroy this(PlayerName) activity
                    finish();
                    
                    start_chime.start();
                
                }
            }
        });
    }
}