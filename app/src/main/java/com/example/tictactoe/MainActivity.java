package com.example.tictactoe;

import static com.example.tictactoe.R.*;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
{
    private LinearLayout player1Layout, player2Layout;
    private ImageView image1, image2, image3, image4, image5, image6, image7, image8, image9;
    private TextView player1TV, player2TV, player1pointTV,player2pointTV, tieTV;
    
    // tie counter
    private int tieScore = 0;
    private int player1Score = 0;
    private int player2Score = 0;
    
    // winning combinations
    private final List<int[]> combinationsList = new ArrayList<>();
    
    // done boxes positions by players so players can't select them again
    private List<String> doneBoxes = new ArrayList<>();
    
    // player unique ID
    private String playerUniqueId = "0";
    
    // getting firebase database reference from URL
    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReferenceFromUrl("https://tictactoe-ce6a0-default-rtdb.firebaseio.com/");
    
    // true when opponent will be found to play the game
    private boolean opponentFound = false;
    
    // opponent unique ID
    private String opponentUniqueId = "0";
    
    // values must be matching or waiting
    // when a player creates a new connection/room and he's waiting for another player to join then the value will be waiting
    private String status = "matching";
    
    // player turn
    private String playerTurn = "";
    
    // opponent chime
    private static MediaPlayer o_chime;
    private static MediaPlayer win_chime;
    private static MediaPlayer lose_chime;
    
    // connection ID in which the player has joined to play the game
    private String connectionID = "";
    
    // room ID
    private String roomId = "";
    
    // room ID for rematching
    private String roomIdCheck = "";
    
    // playerName from previous game
    private String playerName = "";
    
    // Generating ValueEventListeners for Firebase Database
    // turnsEventListener listens for the players turns
    // wonEventListener listens if the player has won the match
    ValueEventListener turnsEventListener, wonEventsListener;
    
    // selected boxes by players
    // empty fields will be replaced by player ids
    private String[] boxesSelectedBy = {"","","","","","","","",""};
    
    // setting the playerWon to false as initialization
    private boolean isPlayerWon = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        
        super.onCreate(savedInstanceState);
        setContentView(layout.activity_main);
        
//        final MediaPlayer o_chime = MediaPlayer.create(this, raw.o_bell);
        final MediaPlayer x_chime = MediaPlayer.create(this, raw.x_bell);
    
        // getting PlayerName from PlayerName.class file
        final String getPlayerName = getIntent().getStringExtra("playerName");
        playerName = getPlayerName;
    
        // getting rematch from WinDialog.class file
        final boolean rematch = getIntent().getBooleanExtra("reMatch",false);
        if (rematch)
        {
//            roomIdCheck = getIntent().getStringExtra("roomId");
//            boxesSelectedBy = getIntent().getStringArrayExtra("boxesSelectedBy");
//            doneBoxes = getIntent().getStringArrayListExtra("doneBoxes");
//            playerUniqueId = getIntent().getStringExtra("playerUniqueId");
//            playerUniqueId = String.valueOf(System.currentTimeMillis());
            player1Score = getIntent().getIntExtra("playerScore",0);
            player2Score = getIntent().getIntExtra("opponentScore",0);
            tieScore = getIntent().getIntExtra("tieScore",0);
        }
        
            // generate player unique id, Player will be identified by this id
            playerUniqueId = String.valueOf(System.currentTimeMillis());
        
        
        player1Layout = findViewById(id.player1Layout);
        player2Layout = findViewById(id.player2Layout);
        
        image1 = findViewById(id.image1);
        image2 = findViewById(id.image2);
        image3 = findViewById(id.image3);
        image4 = findViewById(id.image4);
        image5 = findViewById(id.image5);
        image6 = findViewById(id.image6);
        image7 = findViewById(id.image7);
        image8 = findViewById(id.image8);
        image9 = findViewById(id.image9);
    
        player1TV = findViewById(id.player1TV);
        player2TV = findViewById(id.player2TV);
    
        player1pointTV = findViewById(id.player1pointTV);
        player2pointTV = findViewById(id.player2pointTV);
    
        tieTV = findViewById(id.tieTV);
        
        // generating winning combinations
        // horizontal wins
        combinationsList.add(new int[]{0,1,2});
        combinationsList.add(new int[]{3,4,5});
        combinationsList.add(new int[]{6,7,8});
        
        // vertical wins
        combinationsList.add(new int[]{0,3,6});
        combinationsList.add(new int[]{1,4,7});
        combinationsList.add(new int[]{2,5,8});
    
        // diagonal wins
        combinationsList.add(new int[]{0,4,8});
        combinationsList.add(new int[]{2,4,6});
    
        // showing progress dialog while waiting for opponent
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);
        progressDialog.setMessage("Waiting for Opponent");
        progressDialog.show();
        
        // clearing the database
//        databaseReference=FirebaseDatabase.getInstance().getReference("connections");
//        databaseReference.setValue(null);
        
        
        // setting the player name to the TextView
        player1TV.setText(getPlayerName);
        player1pointTV.setText(String.valueOf(player1Score));
        player2pointTV.setText(String.valueOf(player2Score));
        tieTV.setText(String.valueOf(tieScore));
        
        databaseReference.child("connections").addValueEventListener(new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
                // check if opponent found or not, if not then look for the opponent
                if (!opponentFound)
                {
                    // checking if there are other players in the firebase realtime database
                    if (snapshot.hasChildren() || rematch)
                    {
                        // checking all connections if other users are also waiting for a user to play the match
                        for (DataSnapshot connections : snapshot.getChildren())
                        {
                            // getting connection unique ID
                            roomId = connections.getKey();
    
                            String conId = roomId;
                            
                            // 2 players are required to start the game
                            // if getPlayersCount is 1 it means that there is another player waiting for an opponent to play the game
                            // else if getPlayersCount is 2 it means this connection was established between the 2 players
                            int getPlayersCount = (int)connections.getChildrenCount();
                            
                            // after creating a new connection waiting for the other player to join
                            if (status.equals("waiting"))
                            {
                                // if getPlayersCount is 2 means other player joined the match
                                if (getPlayersCount == 2)
                                {
                                    playerTurn = playerUniqueId;
                                    applyPlayerTurn(playerTurn);
                                    
                                    // true when opponent found in connections
                                    boolean playerFound = false;
                                    
                                    // getting players in connection
                                    for(DataSnapshot players : connections.getChildren())
                                    {
                                        String get2ndPlayerUniqueID = players.getKey();
                                        
                                        // check if player id match with player who created connection (this player)
                                        // if match then get opponent details
                                        if (get2ndPlayerUniqueID.equals(playerUniqueId))
                                        {   // traceeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee
                                            playerFound = true;
                                        }
                                        else if (playerFound)
                                        {
                                            String getOpponentPlayerName = players.child("player_name").getValue(String.class);
                                            opponentUniqueId = players.getKey();
                                            
                                            // set opponent playername to TextView
                                            player2TV.setText(getOpponentPlayerName);
                                            
                                            // assigning connection/room ID
                                            connectionID = conId;
                                            opponentFound = true;
                                            
                                            // adding turns listener and won listener to the database reference
                                            databaseReference.child("turns").child(connectionID).addValueEventListener(turnsEventListener);
                                            databaseReference.child("won").child(connectionID).addValueEventListener(wonEventsListener);
                                            
                                            // hide progress dialog if showing
                                            if (progressDialog.isShowing())
                                            {
                                                progressDialog.dismiss();
                                            }
                                            
                                            // once the connection is made, remove connectionListener from Database Reference
                                            databaseReference.child("connections").removeEventListener(this);
                                            
                                        }
                                    }
                                }
                            }
                            
                            // in case the player has not created the connection/room because of other rooms are not available to join
                            else
                            {
                                // checking if the connections has 1 player waiting and needs 1 more player to play then join this connection
                                if (getPlayersCount == 1)
                                {
                                    // add player to this connection
                                    connections.child(playerUniqueId).child("player_name").getRef().setValue(getPlayerName);
                                    
                                    // getting both players
                                    for (DataSnapshot players : connections.getChildren())
                                    {
                                        String getOpponentName = players.child("player_name").getValue(String.class);
                                        opponentUniqueId = players.getKey();
                                        
                                        // first turn will be of who created the connection/room
                                        playerTurn = opponentUniqueId;
                                        applyPlayerTurn(playerTurn);
                                        
                                        // setting the opponent's playername to the TextView
                                        player2TV.setText(getOpponentName);
                                        
                                        // assigning connection ID
                                        connectionID = conId;
                                        opponentFound = true;
    
                                        // adding turns listener and won listener to the database reference
                                        databaseReference.child("turns").child(connectionID).addValueEventListener(turnsEventListener);
                                        databaseReference.child("won").child(connectionID).addValueEventListener(wonEventsListener);
    
                                        // hide progress dialog if showing
                                        if (progressDialog.isShowing())
                                        {
                                            progressDialog.dismiss();
                                        }
    
                                        // once the connection has made remove connectionListener from Database Reference
                                        databaseReference.child("connections").removeEventListener(this);
                                        
                                        break;
                                    }
                                }
                            }
                        }
    
                        // check if opponent is not found and user is not waiting for the opponent anymore
                        // then create a new connection
                        if (!opponentFound && !status.equals("waiting"))
                        {
                            // generating unique ID for the connection
                            String connectionUniqueId = String.valueOf(System.currentTimeMillis());
        
                            // adding first player to the connection and waiting for another player to connect and play
                            snapshot.child(connectionUniqueId).child(playerUniqueId).child("player_name").getRef().setValue(getPlayerName);
        
                            status = "waiting";
                        }
                    }
                    // if there is no connection available in the firebase database then create a new connection
                    // it is like creating a new room and waiting for another player to join the room
                    else
                    {
                        // generating unique ID for the connection
                        String connectionUniqueId = String.valueOf(System.currentTimeMillis());
    
                        // adding first player to the connection and waiting for another player to connect and play
                        snapshot.child(connectionUniqueId).child(playerUniqueId).child("player_name").getRef().setValue(getPlayerName);
    
                        status = "waiting";
                    }
                }
            }
    
            @Override
            public void onCancelled(@NonNull DatabaseError error)
            {
        
            }
        });
        
        turnsEventListener = new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
                // getting all turns of the connection
                for (DataSnapshot dataSnapshot : snapshot.getChildren())
                {
                    if (dataSnapshot.getChildrenCount() == 2)
                    {
                        // getting box position selection by the player
                        final int getBoxPosition = Integer.parseInt(dataSnapshot.child("box_position").getValue(String.class));
                        
                        // getting player id who selected the box
                        final String getPlayerID = dataSnapshot.child("player_id").getValue(String.class);
                        
                        // checking if the player has not selected the box before
                        // to avoid connection errors
                        if (!doneBoxes.contains(String.valueOf(getBoxPosition)))
                        {
                            // select the box
                            doneBoxes.add(String.valueOf(getBoxPosition));
                            
                            if (getBoxPosition == 1)
                            {
                                selectBox(image1, getBoxPosition, getPlayerID);
                            }
                            else if (getBoxPosition == 2)
                            {
                                selectBox(image2, getBoxPosition, getPlayerID);
                            }
                            else if (getBoxPosition == 3)
                            {
                                selectBox(image3, getBoxPosition, getPlayerID);
                            }
                            else if (getBoxPosition == 4)
                            {
                                selectBox(image4, getBoxPosition, getPlayerID);
                            }
                            else if (getBoxPosition == 5)
                            {
                                selectBox(image5, getBoxPosition, getPlayerID);
                            }
                            else if (getBoxPosition == 6)
                            {
                                selectBox(image6, getBoxPosition, getPlayerID);
                            }
                            else if (getBoxPosition == 7)
                            {
                                selectBox(image7, getBoxPosition, getPlayerID);
                            }
                            else if (getBoxPosition == 8)
                            {
                                selectBox(image8, getBoxPosition, getPlayerID);
                            }
                            else if (getBoxPosition == 9)
                            {
                                selectBox(image9, getBoxPosition, getPlayerID);
                            }
                        }
                    }
                }
            }
    
            @Override
            public void onCancelled(@NonNull DatabaseError error)
            {
        
            }
        };
        
        wonEventsListener = new ValueEventListener()
        {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
                // check if a player has won the match
                if (snapshot.hasChild("player_id"))
                {
                    String getWinPlayerID = snapshot.child("player_id").getValue(String.class);
                    
                    final WinDialog winDialog;
                    
                    if (getWinPlayerID.equals(playerUniqueId))
                    {
                        player1Score++;
                        // show win dialog
                        winDialog = new WinDialog(MainActivity.this, "You have won the game", connectionID, getPlayerName, playerUniqueId, player1Score, player2Score, tieScore);
//                        setPlayerScore(playerUniqueId);
//                        playWinSound(this);
                        // update player 1 score
//                        player1Score++;
//                        player1pointTV.setText(player1Score);
                    }
                    else
                    {
                        player2Score++;
                        // show win dialog
                        winDialog = new WinDialog(MainActivity.this, "You have lost the game", connectionID, getPlayerName, playerUniqueId, player1Score, player2Score, tieScore);
//                        playLoseSound(this);
                        // update player 2 score
//                        player2Score++;
//                        player2pointTV.setText(player2Score);
                    }
                    
                    winDialog.setCancelable(false);
                    winDialog.show();
                    
                    // remove listeners from DataBase
                    databaseReference.child("turns").child(connectionID).removeEventListener(turnsEventListener);
                    databaseReference.child("won").child(connectionID).removeEventListener(wonEventsListener);
                }
            }
    
//            private void playWinSound(ValueEventListener valueEventListener)
//            {
//                win_chime = MediaPlayer.create((Context) valueEventListener, raw.win_bell);
//                win_chime.start();
//            }
//
//            private void playLoseSound(ValueEventListener valueEventListener)
//            {
//                lose_chime = MediaPlayer.create((Context) valueEventListener, raw.lose_bell);
//                lose_chime.start();
//            }
    
            @Override
            public void onCancelled(@NonNull DatabaseError error)
            {
        
            }
        };
        
        image1.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // check if the box is not selected before & current player's turn
                if (!doneBoxes.contains("1") && playerTurn.equals(playerUniqueId))
                {
                    ((ImageView)v).setImageResource(drawable.new_x_icon);
                    
                    // play x chime
                    x_chime.start();
                    
                    // send selected box positions and player unique id to Firebase Database
                    databaseReference.child("turns").child(connectionID).child(String.valueOf(doneBoxes.size() + 1)).child("box_position").setValue("1");
                    databaseReference.child("turns").child(connectionID).child(String.valueOf(doneBoxes.size() + 1)).child("player_id").setValue(playerUniqueId);
                
                    // change player turn
                    playerTurn = opponentUniqueId;
                }
            }
        });
    
        image2.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // check if the box is not selected before & current player's turn
                if (!doneBoxes.contains("2") && playerTurn.equals(playerUniqueId))
                {
                    ((ImageView)v).setImageResource(drawable.new_x_icon);
    
                    // play x chime
                    x_chime.start();
                    
                    // send selected box positions and player unique id to Firebase Database
                    databaseReference.child("turns").child(connectionID).child(String.valueOf(doneBoxes.size() + 1)).child("box_position").setValue("2");
                    databaseReference.child("turns").child(connectionID).child(String.valueOf(doneBoxes.size() + 1)).child("player_id").setValue(playerUniqueId);
                
                    // change player turn
                    playerTurn = opponentUniqueId;
                }
            }
        });
    
        image3.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // check if the box is not selected before & current player's turn
                if (!doneBoxes.contains("3") && playerTurn.equals(playerUniqueId))
                {
                    ((ImageView)v).setImageResource(drawable.new_x_icon);
    
                    // play x chime
                    x_chime.start();
                    
                    // send selected box positions and player unique id to Firebase Database
                    databaseReference.child("turns").child(connectionID).child(String.valueOf(doneBoxes.size() + 1)).child("box_position").setValue("3");
                    databaseReference.child("turns").child(connectionID).child(String.valueOf(doneBoxes.size() + 1)).child("player_id").setValue(playerUniqueId);
                
                    // change player turn
                    playerTurn = opponentUniqueId;
                }
            }
        });
        
        image4.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // check if the box is not selected before & current player's turn
                if (!doneBoxes.contains("4") && playerTurn.equals(playerUniqueId))
                {
                    ((ImageView)v).setImageResource(drawable.new_x_icon);
    
                    // play x chime
                    x_chime.start();
                    
                    // send selected box positions and player unique id to Firebase Database
                    databaseReference.child("turns").child(connectionID).child(String.valueOf(doneBoxes.size() + 1)).child("box_position").setValue("4");
                    databaseReference.child("turns").child(connectionID).child(String.valueOf(doneBoxes.size() + 1)).child("player_id").setValue(playerUniqueId);
                
                    // change player turn
                    playerTurn = opponentUniqueId;
                }
            }
        });
        
        image5.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // check if the box is not selected before & current player's turn
                if (!doneBoxes.contains("5") && playerTurn.equals(playerUniqueId))
                {
                    ((ImageView)v).setImageResource(drawable.new_x_icon);
    
                    // play x chime
                    x_chime.start();
                    
                    // send selected box positions and player unique id to Firebase Database
                    databaseReference.child("turns").child(connectionID).child(String.valueOf(doneBoxes.size() + 1)).child("box_position").setValue("5");
                    databaseReference.child("turns").child(connectionID).child(String.valueOf(doneBoxes.size() + 1)).child("player_id").setValue(playerUniqueId);
                
                    // change player turn
                    playerTurn = opponentUniqueId;
                }
            }
        });
        
        image6.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // check if the box is not selected before & current player's turn
                if (!doneBoxes.contains("6") && playerTurn.equals(playerUniqueId))
                {
                    ((ImageView)v).setImageResource(drawable.new_x_icon);
    
                    // play x chime
                    x_chime.start();
                    
                    // send selected box positions and player unique id to Firebase Database
                    databaseReference.child("turns").child(connectionID).child(String.valueOf(doneBoxes.size() + 1)).child("box_position").setValue("6");
                    databaseReference.child("turns").child(connectionID).child(String.valueOf(doneBoxes.size() + 1)).child("player_id").setValue(playerUniqueId);
                
                    // change player turn
                    playerTurn = opponentUniqueId;
                }
            }
        });
        
        image7.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // check if the box is not selected before & current player's turn
                if (!doneBoxes.contains("7") && playerTurn.equals(playerUniqueId))
                {
                    ((ImageView)v).setImageResource(drawable.new_x_icon);
    
                    // play x chime
                    x_chime.start();
                    
                    // send selected box positions and player unique id to Firebase Database
                    databaseReference.child("turns").child(connectionID).child(String.valueOf(doneBoxes.size() + 1)).child("box_position").setValue("7");
                    databaseReference.child("turns").child(connectionID).child(String.valueOf(doneBoxes.size() + 1)).child("player_id").setValue(playerUniqueId);
                
                    // change player turn
                    playerTurn = opponentUniqueId;
                }
            }
        });
        
        image8.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // check if the box is not selected before & current player's turn
                if (!doneBoxes.contains("8") && playerTurn.equals(playerUniqueId))
                {
                    ((ImageView)v).setImageResource(drawable.new_x_icon);
    
                    // play x chime
                    x_chime.start();
                    
                    // send selected box positions and player unique id to Firebase Database
                    databaseReference.child("turns").child(connectionID).child(String.valueOf(doneBoxes.size() + 1)).child("box_position").setValue("8");
                    databaseReference.child("turns").child(connectionID).child(String.valueOf(doneBoxes.size() + 1)).child("player_id").setValue(playerUniqueId);
                
                    // change player turn
                    playerTurn = opponentUniqueId;
                }
            }
        });
    
        image9.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // check if the box is not selected before & current player's turn
                if (!doneBoxes.contains("9") && playerTurn.equals(playerUniqueId))
                {
                    ((ImageView)v).setImageResource(drawable.new_x_icon);
    
                    // play x chime
                    x_chime.start();
                    
                    // send selected box positions and player unique id to Firebase Database
                    databaseReference.child("turns").child(connectionID).child(String.valueOf(doneBoxes.size() + 1)).child("box_position").setValue("9");
                    databaseReference.child("turns").child(connectionID).child(String.valueOf(doneBoxes.size() + 1)).child("player_id").setValue(playerUniqueId);
                
                    // change player turn
                    playerTurn = opponentUniqueId;
                }
            }
        });
    }
    
    private void applyPlayerTurn(String playerUniqueId2)
    {
        if (playerUniqueId2.equals(playerUniqueId))
        {
            // check backgrounds
            player2TV.setTextColor(getResources().getColor(color.grey));
            player1TV.setTextColor(getResources().getColor(color.white));
        }
        else
        {
            // check backgrounds
            player2TV.setTextColor(getResources().getColor(color.white));
            player1TV.setTextColor(getResources().getColor(color.grey));
        }
    
    }
    
    private void selectBox(ImageView imageView, int selectedBoxPosition, String selectedByPlayer)
    {
        boxesSelectedBy[selectedBoxPosition - 1] = selectedByPlayer;
        
        if(selectedByPlayer.equals(playerUniqueId))
        {
            imageView.setImageResource(drawable.new_x_icon);
            playerTurn = opponentUniqueId;
        }
        else
        {
            imageView.setImageResource(drawable.new_o_icon);
            playerTurn = playerUniqueId;
            playOpponentSound(this);
        }
        
        applyPlayerTurn(playerTurn);
        
        // checking whether player has won the match
        if (checkPlayerWin(selectedByPlayer))
        {
            // sending won player unique ID to Firebase Database so opponent can be notified
            databaseReference.child("won").child(connectionID).child("player_id").setValue(selectedByPlayer);
        }
        // checking the game if there are no more boxes to be selected
        if (doneBoxes.size() == 9)
        {
            // updates tie counter
            tieScore++;
            
            final WinDialog winDialog = new WinDialog(MainActivity.this, "It is a Tie!", connectionID, playerName, playerUniqueId, player1Score, player2Score, tieScore);
            winDialog.setCancelable(false);
            winDialog.show();
        
        }
    }
    
    private boolean checkPlayerWin(String playerid)
    {
        
        // compare player turns with every winning combination
        for (int i = 0; i < combinationsList.size(); i++)
        {
            final int[] combination = combinationsList.get(i);
            
            // checking last three turns of user
            if (boxesSelectedBy[combination[0]].equals(playerid) &&
                    boxesSelectedBy[combination[1]].equals(playerid) &&
                    boxesSelectedBy[combination[2]].equals(playerid))
            {
                isPlayerWon = true;
            }
        }
        
        return isPlayerWon;
    }
    
    
    public static void playOpponentSound(Context con)
    {
        o_chime= MediaPlayer.create(con, raw.o_bell);
        o_chime.start();
    }
//    public static void playWinSound(Context con)
//    {
//        win_chime = MediaPlayer.create(con, raw.win_bell);
//        win_chime.start();
//    }
//    public static void playLoseSound(Context con)
//    {
//        lose_chime= MediaPlayer.create(con, raw.lose_bell);
//        lose_chime.start();
//    }
    
    public void setPlayerScore(String playerUniqueId)
    {
        if (playerTurn.equals(playerUniqueId))
        {
            player1Score++;
            player1pointTV.setText(player1Score);
        }
        else
        {
            player2Score++;
            player2pointTV.setText(player2Score);
        }
    }
}