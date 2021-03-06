package com.decisionkitchen.decisionkitchen

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.opengl.Visibility
import android.os.Bundle
import android.support.v4.app.ShareCompat
import android.support.v7.widget.Toolbar
import android.text.Layout
import android.util.Log
import android.view.*
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ValueEventListener
import android.widget.*
import com.facebook.drawee.view.SimpleDraweeView
import com.google.firebase.auth.FirebaseAuth
import com.facebook.drawee.generic.RoundingParams
import com.github.kevinsawicki.timeago.TimeAgo
import com.usebutton.sdk.ButtonContext
import com.usebutton.sdk.ButtonDropin
import com.usebutton.sdk.context.Location
import com.usebutton.sdk.util.LocationProvider
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import org.joda.time.format.ISODateTimeFormat
import java.net.URLEncoder

class GroupActivity : Activity() {

    private fun getContext(): Context {
        return this
    }

    private var mShareActionProvider: ShareActionProvider? = null

    private var group: Group? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        com.usebutton.sdk.Button.getButton(this).start();

        setContentView(R.layout.activity_group)

        val database = FirebaseDatabase.getInstance()

        val groupRef = database.getReference("groups/" + intent.getStringExtra("GROUP_ID"))

        val toolbar : Toolbar = findViewById(R.id.toolbar) as Toolbar
        toolbar.title = "Loading..."

        val groupListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                group = dataSnapshot.getValue<Group>(Group::class.java)!!
                val memberScrollView : HorizontalScrollView = findViewById(R.id.members) as HorizontalScrollView
                memberScrollView.removeAllViews()
                toolbar.title = group!!.name

                var loadCount : Int = 0

                val auth: FirebaseAuth = FirebaseAuth.getInstance()

                val roundingParams = RoundingParams.fromCornersRadius(5f)
                roundingParams.roundAsCircle = true

                val membersLayout = LinearLayout(getContext())
                val membersLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                membersLayout.layoutParams = membersLayoutParams
                membersLayout.orientation = LinearLayout.HORIZONTAL
                for (member_id in group!!.members!!) {
                    database.getReference("users/" + member_id).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(memberSnapshot: DataSnapshot) {

                            val userLayout = LinearLayout(membersLayout.context)
                            userLayout.orientation = LinearLayout.VERTICAL
                            userLayout.gravity = Gravity.CENTER_HORIZONTAL
                            userLayout.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT)

                            val member : User = memberSnapshot.getValue<User>(User::class.java)!!

                            val profile = SimpleDraweeView(userLayout.context)
                            profile.setImageURI(member.img)
                            profile.hierarchy.roundingParams = roundingParams
                            val params = ViewGroup.LayoutParams(120, 120)
                            profile.layoutParams = params;
                            val marginParams: LinearLayout.LayoutParams = LinearLayout.LayoutParams(profile.layoutParams);
                            marginParams.setMargins(50, 30, 50, 20)
                            profile.layoutParams = marginParams
                            userLayout.addView(profile)

                            val name = TextView(userLayout.context)
                            name.text = member.first_name
                            name.textSize = 11.0F
                            name.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT)
                            name.setPadding(0, 0, 0, 30)
                            userLayout.addView(name)

                            membersLayout.addView(userLayout)

                            loadCount ++

                            if (loadCount == group!!.members!!.size) {
                                findViewById(R.id.loader).visibility = View.INVISIBLE
                                findViewById(R.id.content).visibility = View.VISIBLE
                            }

                        }
                        override fun onCancelled(databaseError: DatabaseError) {}
                    })
                }
                memberScrollView.addView(membersLayout)

                val mainContent : LinearLayout = findViewById(R.id.group_content) as LinearLayout
                mainContent.removeAllViews()

                if (group!!.games != null) {
                    for (game in group!!.games!!) {

                        Log.w("test", game.toString())

                        if (game.meta!!.end == null) {
                            if (game.responses != null && game.responses.containsKey(FirebaseAuth.getInstance().currentUser!!.uid)) {
                                findViewById(R.id.create_game).visibility = View.GONE
                                findViewById(R.id.voted).visibility = View.VISIBLE
                            } else {
                                (findViewById(R.id.create_game) as TextView).setText(R.string.join_vote)
                                (findViewById(R.id.textView) as TextView).setText("Enter your vote!")
                            }
                            continue
                        }



                        val cardWrapper = LinearLayout(mainContent.context)
                        cardWrapper.orientation = LinearLayout.VERTICAL
                        cardWrapper.gravity = Gravity.TOP
                        cardWrapper.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)


                        val startDate = TextView(cardWrapper.context)
                        startDate.text = DateTime(game.meta!!.end).toString(DateTimeFormat.shortDateTime())
                        startDate.textSize = 13.0F
                        startDate.typeface = Typeface.create("sans-serif", Typeface.BOLD)
                        startDate.setTextColor(Color.rgb(0, 0, 0))

                        val startDateParams: LinearLayout.LayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT)
                        startDateParams.setMargins(50, 50, 50, 0)
                        startDate.layoutParams = startDateParams
                        cardWrapper.addView(startDate)

                        val cardScrollWrapper = HorizontalScrollView(cardWrapper.context)

                        val cardScrollWrapperInner = LinearLayout(mainContent.context)
                        cardScrollWrapperInner.orientation = LinearLayout.HORIZONTAL
                        cardScrollWrapperInner.gravity = Gravity.TOP
                        cardScrollWrapperInner.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)


                        for (j in (game.result!!).size - 1 downTo 0) {

                            val restaurant = group!!.restaurants!![game.result!![j][0]]!!

                            val cardLayout = LinearLayout(mainContent.context)
                            cardLayout.orientation = LinearLayout.HORIZONTAL
                            cardLayout.gravity = Gravity.TOP
                            cardLayout.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)

                            val profile = SimpleDraweeView(cardLayout.context)
                            profile.setImageURI(if (restaurant.image != null) restaurant.image else "https://unsplash.it/200")
                            profile.hierarchy.roundingParams = roundingParams
                            val params = ViewGroup.LayoutParams(230, 230)
                            profile.layoutParams = params;
                            val marginParams: LinearLayout.LayoutParams = LinearLayout.LayoutParams(profile.layoutParams);
                            marginParams.setMargins(50, 50, 50, 50)
                            profile.layoutParams = marginParams
                            cardLayout.addView(profile)

                            val cardContentLayout = LinearLayout(mainContent.context)
                            cardContentLayout.orientation = LinearLayout.VERTICAL
                            cardContentLayout.gravity = Gravity.TOP
                            cardContentLayout.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                            (cardContentLayout.layoutParams as LinearLayout.LayoutParams).setMargins(0, 50, 50, 50)

                            val name = TextView(cardContentLayout.context)
                            name.text = restaurant.name
                            name.textSize = 20.0F
                            name.typeface = Typeface.DEFAULT_BOLD
                            name.setTextColor(Color.BLACK)
                            name.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT)
                            name.setPadding(0, 0, 0, 10)
                            cardContentLayout.addView(name)

                            val address = TextView(cardContentLayout.context)
                            address.text = restaurant.address
                            address.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT)
                            address.setPadding(0, 0, 0, 20)
                            cardContentLayout.addView(address)
/*
                            val date = TextView(cardContentLayout.context)
                            date.text = DateTime(game.meta!!.end).toString(DateTimeFormat.shortDateTime())
                            date.textSize = 15.0F
                            date.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
                            date.setTextColor(Color.rgb(150, 150, 150))
                            date.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT)
                            cardContentLayout.addView(date)*/

                            val butt = ButtonDropin(cardContentLayout.context)
                            butt.setButtonId("btn-749e2bd5d9e4a4c3")
                            // Set Button Context
                            val location : com.usebutton.sdk.context.Location = com.usebutton.sdk.context.Location(restaurant.name)
                            location.setAddress(restaurant.address)
                            location.setCity(restaurant.city)
                            location.setState(restaurant.state)
                            location.setZip(restaurant.zip)
                            location.setName(restaurant.name)
                            location.setCountry("USA")
                            val context = ButtonContext.withSubjectLocation(location)

                              // Provide the user's location if we have the permission to
                            try {
                                @SuppressLint("MissingPermission")
                                val userLocation: android.location.Location? = com.usebutton.sdk.util.LocationProvider(applicationContext).bestLocation
                                if (userLocation != null) {
                                    context.setUserLocation(userLocation as com.usebutton.sdk.context.Location);
                                }
                            } catch (e: Exception) {

                            }

                              // Prepare the Button for display with our Context
                            butt.prepareForDisplay(context);

                            cardContentLayout.addView(butt)

                            cardLayout.addView(cardContentLayout)

                            cardScrollWrapperInner.addView(cardLayout)
                        }

                        cardScrollWrapper.addView(cardScrollWrapperInner)

                        cardWrapper.addView(cardScrollWrapper)

                        mainContent.addView(cardWrapper)

                    }
                }

            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("dk", "loadPost:onCancelled", databaseError.toException())
                // ...
            }
        }

        toolbar.setNavigationOnClickListener {
            groupRef.removeEventListener(groupListener)
            val intent: Intent = Intent(applicationContext, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            applicationContext.startActivity(intent)
        }

        groupRef.addValueEventListener(groupListener)
    }

    public fun createGame(view: View) {
        val intent:Intent = Intent(getBaseContext(), RestaurantActivity::class.java)
        intent.putExtra("GROUP_ID", getIntent().getStringExtra("GROUP_ID"))
        startActivity(intent)

        val ref = FirebaseDatabase.getInstance().getReference("groups/" + getIntent().getStringExtra("GROUP_ID"))
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                Log.e("error", p0.toString())
            }

            override fun onDataChange(snapshot: DataSnapshot) {
                val group = snapshot.getValue(Group::class.java)!!

                if (group.games != null) {
                    for (game in group.games!!) {
                        if (game.meta!!.end == null) {
                            return
                        }
                    }
                    val game = Game(GameMeta(null, ISODateTimeFormat.dateTimeNoMillis().print(DateTime())))
                    group.games!!.add(game)
                    ref.setValue(group)
                    return
                }

                val al = ArrayList<Game>()
                al.add(Game(GameMeta(null, ISODateTimeFormat.dateTimeNoMillis().print(DateTime()))))
                val newGroup = Group(group.password, group.members, group.name, group.restaurants, group.id, al)
                ref.setValue(newGroup)
            }

        })
    }

    override public fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate menu resource file.
        menuInflater.inflate(R.menu.group_activity_actions, menu);
        val item = menu.findItem(R.id.share);
        mShareActionProvider = item.actionProvider as ShareActionProvider;
        // Create the share Intent
        val url = "https://decisionkitchen.app/group?name=" + URLEncoder.encode(group!!.name) + "&pass=" + URLEncoder.encode(group!!.password)
        val shareText = "Join my DecisionKitchen group at " + url;
        val shareIntent = ShareCompat.IntentBuilder.from(this).setType("text/plain").setText(shareText).getIntent();
        // Set the share Intent
        (mShareActionProvider as ShareActionProvider).setShareIntent(shareIntent);
        return true;
    }

}