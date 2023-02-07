package com.example.tictactoe;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class WinDialog extends Dialog
{
    private final String message;
    private final MainActivity mainActivity;
    private final String connectionID;
    private final String playerName;
    private final String playerUniqueId;
    private final int playerScore;
    private final int opponentScore;
    private final int tieScore;
    
    public WinDialog(@NonNull Context context, String message, String connectionID, String playerName, String playerUniqueId, int playerScore, int opponentScore, int tieScore)
    {
        super(context);
        this.message = message;
        this.connectionID = connectionID;
        this.playerName = playerName;
        this.playerUniqueId = playerUniqueId;
        this.playerScore = playerScore;
        this.opponentScore = opponentScore;
        this.tieScore = tieScore;
        this.mainActivity = ((MainActivity) context);
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.win_dialog_layout);
        
        final TextView messageTV = findViewById(R.id.messageTV);
        final Button startBtn = findViewById(R.id.startNewBtn);
        final Button remBtn = findViewById(R.id.rematchBtn);
        final MediaPlayer start_chime;
    
//        start_chime = MediaPlayer.create(this, R.raw.start_bell);
        
        
        messageTV.setText(message);
        
        startBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                dismiss();
                getContext().startActivity(new Intent(getContext(), PlayerName.class));
                mainActivity.finish();
            }
        });
        
        remBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                String[] boxesSelectedBy = {"", "", "", "", "", "", "", "", ""};
                dismiss();
                Intent intent = new Intent(new Intent(getContext(), MainActivity.class));
                intent.putExtra("roomId",connectionID);
                intent.putExtra("reMatch",true);
                intent.putExtra("playerName",playerName);
                intent.putExtra("playerScore",playerScore);
                intent.putExtra("opponentScore",opponentScore);
                intent.putExtra("tieScore",tieScore);
                intent.putExtra("boxesSelectedBy", new String[]{"", "", "", "", "", "", "", "", ""});
                intent.putExtra("playerUniqueId",playerUniqueId);
                intent.putStringArrayListExtra("doneBoxes",new ArrayList<>());
                getContext().startActivity(intent);
                mainActivity.finish();
//                start_chime.start();
            }
        });
        
    }
}
