package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Share
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.BookingEntity
import com.example.ui.theme.*

@Composable
fun InviteDialog(
    booking: BookingEntity,
    viewModelHelper: com.example.viewmodel.BookingViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var senderEmail by remember { mutableStateOf(booking.senderEmail) }
    var currentEmailInput by remember { mutableStateOf("") }
    val attendeeEmails = remember {
        mutableStateListOf<String>().apply {
            addAll(viewModelHelper.parseAttendeeEmails(booking.attendees))
        }
    }

    val roomLabel = if (booking.roomName.lowercase() == "gyaan") "Gyaan Conference Room" else "Dharma Conference Room"
    val floorLabel = if (booking.roomName.lowercase() == "gyaan") "2nd Floor" else "1st Floor"

    // Construct preview invite text dynamically
    val previewText = remember(booking, senderEmail, attendeeEmails.size) {
        val meetingSubject = booking.meetingSubject.ifEmpty { "(no subject)" }
        val bookedBy = booking.bookedBy.ifEmpty { "—" }
        val dept = booking.department.ifEmpty { "—" }
        
        val dateStr = viewModelHelper.fmtDate(booking.startTime)
        val startStr = viewModelHelper.fmtTime(booking.startTime)
        val endStr = if (booking.endTime > 0L) " – " + viewModelHelper.fmtTime(booking.endTime) else ""
        val dateTimeString = "$dateStr $startStr$endStr"
        
        val sentBy = senderEmail.ifEmpty { "—" }
        val sentTo = if (attendeeEmails.isEmpty()) "(add attendees above)" else attendeeEmails.joinToString(", ")

        """
        Meeting Invite — $meetingSubject
        Room:      $roomLabel, $floorLabel
        Department: $dept
        Date/Time: $dateTimeString
        Booked by: $bookedBy
        Invite by: $sentBy
        To:        $sentTo

        You are invited to attend the above meeting.
        Please confirm your attendance.
        """.trimIndent()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("invite_modal_box"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📧 Invite Attendees",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_invite_modal")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close description")
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Your Email field
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Your email (optional)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = senderEmail,
                        onValueChange = { senderEmail = it },
                        placeholder = { Text("yourname@advaitgroup.co.in", fontSize = 13.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("invite_sender_email"),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                }

                // Attendee Emails field
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Attendee emails — press enter/space to add",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = currentEmailInput,
                        onValueChange = { input ->
                            if (input.endsWith(" ") || input.endsWith(",") || input.endsWith(";")) {
                                val clean = input.trim().replace(",", "").replace(";", "")
                                if (clean.isNotEmpty() && clean.contains("@")) {
                                    if (!attendeeEmails.contains(clean)) {
                                        attendeeEmails.add(clean)
                                    }
                                    currentEmailInput = ""
                                }
                            } else {
                                currentEmailInput = input
                            }
                        },
                        placeholder = { Text("email@advaitgroup.co.in", fontSize = 13.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("invite_attendee_input"),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Chips layout
                    if (attendeeEmails.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(attendeeEmails) { email ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = SleekPrimaryContainer,
                                    modifier = Modifier.testTag("chip_$email")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = email,
                                            fontSize = 11.sp,
                                            color = SleekOnPrimaryContainer
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = SleekOnPrimaryContainer,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable { attendeeEmails.remove(email) }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "No attendees added yet. Add at least one email to send.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                // Preview Box
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Invite preview",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = previewText,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        )
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Copy Button
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(previewText))
                            Toast.makeText(context, "Copied invite to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SleekSecondaryContainer,
                            contentColor = SleekPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("copy_invite_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("📋 Copy", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Email / Send Mail Button
                    Button(
                        onClick = {
                            // Ensure at least one email is added
                            if (attendeeEmails.isEmpty()) {
                                Toast.makeText(context, "Add attendee emails first!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            // Save current details first
                            viewModelHelper.saveInviteInfo(booking.id, senderEmail, attendeeEmails)

                            // Open mail intent
                            try {
                                val subject = "Meeting Invite — ${booking.meetingSubject.ifEmpty { "Meeting" }}"
                                val mailTo = "mailto:${attendeeEmails.joinToString(",")}" +
                                        "?subject=${Uri.encode(subject)}" +
                                        "&body=${Uri.encode(previewText)}"
                                val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse(mailTo))
                                context.startActivity(Intent.createChooser(emailIntent, "Send invite via..."))
                                onDismiss()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error launching email app.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = attendeeEmails.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SleekPrimary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("send_mail_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("📧 Mail", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Calendar / Block Calendar Button
                    Button(
                        onClick = {
                            if (attendeeEmails.isEmpty()) {
                                Toast.makeText(context, "Add attendee emails first!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            // Save current details first
                            viewModelHelper.saveInviteInfo(booking.id, senderEmail, attendeeEmails)

                            val meetingSubject = booking.meetingSubject.ifEmpty { "Meeting" }
                            fun toISO(ms: Long): String {
                                val d = if (ms <= 0L) System.currentTimeMillis() else ms
                                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                                    timeZone = TimeZone.getTimeZone("UTC")
                                }
                                return sdf.format(Date(d))
                            }

                            val startTimeStr = toISO(booking.startTime)
                            val endTimeStr = toISO(if (booking.endTime > 0L) booking.endTime else booking.startTime + 3600000)
                            val notes = "Room: $roomLabel, $floorLabel\nDept: ${booking.department}\nBooked by: ${booking.bookedBy}\n\nBooked via Advait Conference Room Booking."

                            val teamsUrl = "https://teams.microsoft.com/l/meeting/new" +
                                    "?subject=${Uri.encode("Meeting — $meetingSubject")}" +
                                    "&startTime=${Uri.encode(startTimeStr)}" +
                                    "&endTime=${Uri.encode(endTimeStr)}" +
                                    "&content=${Uri.encode(notes)}" +
                                    "&attendees=${Uri.encode(attendeeEmails.joinToString(","))}"

                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(teamsUrl))
                                context.startActivity(intent)
                                onDismiss()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open browser for Teams.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = attendeeEmails.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6264A7), // Teams Purple Color
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(44.dp)
                            .testTag("block_calendar_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("📅 Calendar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "📧 Send Mail — Prepares an email message.\n📅 Calendar — Configures a Microsoft Teams digital calendar block.",
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
