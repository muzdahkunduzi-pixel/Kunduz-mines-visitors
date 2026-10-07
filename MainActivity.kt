package af.gov.momp.visitors

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

data class Visitor(
    val id: Int,
    val name: String,
    val father: String,
    val phone: String,
    val tazkira: String,
    val province: String,
    val district: String,
    val date: String,
    val time: String,
    val department: String,
    val subject: String,
    val responsible: String,
    val photo: String,
    val attachment: String,
    val note: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

@Composable
fun App() {
    var loggedIn by remember { mutableStateOf(false) }
    if (!loggedIn) {
        LoginScreen { loggedIn = true }
    } else {
        MainScreen()
    }
}

@Composable
fun LoginScreen(onLogin: () -> Unit) {
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ministry_logo),
            contentDescription = "لوګو",
            modifier = Modifier.size(130.dp)
        )

        Spacer(Modifier.height(12.dp))
        Text(
            "کندز ولایت د کانونو ریاست",
            style = MaterialTheme.typography.headlineSmall
        )
        Text("د مراجعینو د ثبت سیستم")

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = user,
            onValueChange = { user = it },
            label = { Text("کارن نوم") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = pass,
            onValueChange = { pass = it },
            label = { Text("پاسورډ") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { onLogin() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("ننوتل")
        }

        Spacer(Modifier.height(8.dp))

        Text(
            "یادونه: دا MVP نسخه ده؛ اوس مهال هر کارن/پاسورډ منل کېږي.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var page by remember { mutableStateOf("dashboard") }
    var visitors by remember { mutableStateOf(loadVisitors(ctx)) }
    var editing by remember { mutableStateOf<Visitor?>(null) }

    fun saveList(list: List<Visitor>) {
        visitors = list
        saveVisitors(ctx, list)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("کندز ولایت د کانونو ریاست") }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = page == "dashboard",
                    onClick = { page = "dashboard" },
                    icon = { Text("⌂") },
                    label = { Text("کور") }
                )
                NavigationBarItem(
                    selected = page == "add",
                    onClick = {
                        page = "add"
                        editing = null
                    },
                    icon = { Text("+") },
                    label = { Text("نوی مراجع") }
                )
                NavigationBarItem(
                    selected = page == "list",
                    onClick = { page = "list" },
                    icon = { Text("☷") },
                    label = { Text("مراجعین") }
                )
                NavigationBarItem(
                    selected = page == "reports",
                    onClick = { page = "reports" },
                    icon = { Text("▤") },
                    label = { Text("راپورونه") }
                )
            }
        }
    ) { pad ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
        ) {
            when (page) {
                "dashboard" -> Dashboard(visitors)

                "add" -> VisitorForm(editing) { visitor ->
                    val list = if (editing == null) {
                        visitors + visitor
                    } else {
                        visitors.map { if (it.id == visitor.id) visitor else it }
                    }
                    saveList(list)
                    page = "list"
                    editing = null
                }

                "list" -> VisitorList(
                    v = visitors,
                    onEdit = {
                        editing = it
                        page = "add"
                    },
                    onDelete = {
                        saveList(visitors.filter { item -> item.id != it.id })
                    }
                )

                "reports" -> Reports(visitors)
            }
        }
    }
}

@Composable
fun Dashboard(v: List<Visitor>) {
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val todayCount = v.count { it.date == today }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text("ښه راغلاست", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                "ټول مراجعین",
                v.size.toString(),
                Modifier.weight(1f)
            )
            StatCard(
                "نن",
                todayCount.toString(),
                Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(18.dp))

        Text(
            "د مراجعینو د ثبت، لټون، تعدیل، حذف، PDF او Excel سیستم.",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun StatCard(title: String, value: String, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(title)
            Text(value, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
fun VisitorForm(
    old: Visitor?,
    onSave: (Visitor) -> Unit
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current

    var name by remember { mutableStateOf(old?.name ?: "") }
    var father by remember { mutableStateOf(old?.father ?: "") }
    var phone by remember { mutableStateOf(old?.phone ?: "") }
    var taz by remember { mutableStateOf(old?.tazkira ?: "") }
    var prov by remember { mutableStateOf(old?.province ?: "") }
    var dist by remember { mutableStateOf(old?.district ?: "") }
    var dept by remember { mutableStateOf(old?.department ?: "") }
    var subject by remember { mutableStateOf(old?.subject ?: "") }
    var resp by remember { mutableStateOf(old?.responsible ?: "") }
    var note by remember { mutableStateOf(old?.note ?: "") }
    var photo by remember { mutableStateOf(old?.photo ?: "") }
    var attachment by remember { mutableStateOf(old?.attachment ?: "") }

    val gallery = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        photo = uri?.toString().orEmpty()
    }

    val filePick = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        attachment = uri?.toString().orEmpty()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                if (old == null) "د نوي مراجع ثبت" else "د مراجع معلومات تعدیل",
                style = MaterialTheme.typography.headlineSmall
            )
        }

        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("نوم او تخلص") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = father,
                onValueChange = { father = it },
                label = { Text("د پلار نوم") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("د اړیکې شمېره") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = taz,
                onValueChange = { taz = it },
                label = { Text("د تذکرې نمبر") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = prov,
                onValueChange = { prov = it },
                label = { Text("ولایت") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = dist,
                onValueChange = { dist = it },
                label = { Text("ولسوالي") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = dept,
                onValueChange = { dept = it },
                label = { Text("اداره / مدیریت") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("د راتګ موضوع او غوښتنه") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
        }

        item {
            OutlinedTextField(
                value = resp,
                onValueChange = { resp = it },
                label = { Text("مسئول") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("اضافي یادښت") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = { gallery.launch("image/*") }) {
                    Text("📷 عکس")
                }

                Button(onClick = { filePick.launch("*/*") }) {
                    Text("📎 سند")
                }
            }

            if (photo.isNotBlank()) {
                Text("عکس انتخاب شوی")
            }

            if (attachment.isNotBlank()) {
                Text("سند انتخاب شوی")
            }
        }

        item {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        Toast.makeText(
                            ctx,
                            "نوم ولیکئ",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        val now = Date()

                        onSave(
                            Visitor(
                                id = old?.id
                                    ?: (System.currentTimeMillis() % 100000000).toInt(),
                                name = name.trim(),
                                father = father.trim(),
                                phone = phone.trim(),
                                tazkira = taz.trim(),
                                province = prov.trim(),
                                district = dist.trim(),
                                date = old?.date
                                    ?: SimpleDateFormat(
                                        "yyyy-MM-dd",
                                        Locale.US
                                    ).format(now),
                                time = old?.time
                                    ?: SimpleDateFormat(
                                        "HH:mm",
                                        Locale.US
                                    ).format(now),
                                department = dept.trim(),
                                subject = subject.trim(),
                                responsible = resp.trim(),
                                photo = photo,
                                attachment = attachment,
                                note = note.trim()
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ثبت او ذخیره")
            }
        }
    }
}

@Composable
fun VisitorList(
    v: List<Visitor>,
    onEdit: (Visitor) -> Unit,
    onDelete: (Visitor) -> Unit
) {
    var q by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Visitor?>(null) }

    val query = q.trim()

    val filtered = if (query.isBlank()) {
        v
    } else {
        v.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.phone.contains(query, ignoreCase = true) ||
            it.tazkira.contains(query, ignoreCase = true) ||
            it.id.toString() == query
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        Text(
            "د مراجعینو لست",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = q,
            onValueChange = { q = it },
            label = { Text("نوم، تذکره، موبایل یا ثبت نمبر") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        if (filtered.isEmpty()) {
            Text("هیڅ مراجع پیدا نه شو.")
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = filtered,
                key = { it.id }
            ) { x ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("ثبت نمبر: ${x.id}")
                        Text(
                            x.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text("موضوع: ${x.subject}")
                        Text("${x.date} ${x.time}")

                        Spacer(Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(onClick = { selected = x }) {
                                Text("معلومات")
                            }

                            Button(onClick = { onEdit(x) }) {
                                Text("تعدیل")
                            }

                            OutlinedButton(onClick = { onDelete(x) }) {
                                Text("حذف")
                            }
                        }
                    }
                }
            }
        }
    }

    selected?.let { x ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text("د مراجع بشپړ معلومات") },
            text = {
                Column {
                    Text("نوم: ${x.name}")
                    Text("پلار: ${x.father}")
                    Text("موبایل: ${x.phone}")
                    Text("تذکره: ${x.tazkira}")
                    Text("ولایت/ولسوالي: ${x.province}/${x.district}")
                    Text("اداره: ${x.department}")
                    Text("مسئول: ${x.responsible}")
                    Text("موضوع: ${x.subject}")
                    Text("یادښت: ${x.note}")
                }
            },
            confirmButton = {
                Button(onClick = { selected = null }) {
                    Text("بندول")
                }
            }
        )
    }
}

@Composable
fun Reports(v: List<Visitor>) {
    val ctx = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "راپورونه",
            style = MaterialTheme.typography.headlineSmall
        )

        Button(
            onClick = { exportCsv(ctx, v) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("📊 Excel-compatible CSV صادرول")
        }

        Button(
            onClick = { exportPdf(ctx, v) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("📄 PDF راپور جوړول")
        }

        Button(
            onClick = { backup(ctx, v) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("💾 Backup جوړول")
        }
    }
}

private const val PREFS_NAME = "visitors"
private const val DATA_KEY = "data"

fun prefs(ctx: Context) =
    ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

/*
 * JSON د پخواني "|" پر بنسټ storage پر ځای کارول شوی،
 * څو د پښتو متن، backslash او "|" له امله د معلوماتو د خرابېدو
 * احتمال کم شي.
 */
fun encode(v: Visitor): String {
    return JSONObject().apply {
        put("id", v.id)
        put("name", v.name)
        put("father", v.father)
        put("phone", v.phone)
        put("tazkira", v.tazkira)
        put("province", v.province)
        put("district", v.district)
        put("date", v.date)
        put("time", v.time)
        put("department", v.department)
        put("subject", v.subject)
        put("responsible", v.responsible)
        put("photo", v.photo)
        put("attachment", v.attachment)
        put("note", v.note)
    }.toString()
}

fun decode(s: String): Visitor? {
    return try {
        val o = JSONObject(s)

        Visitor(
            id = o.getInt("id"),
            name = o.optString("name"),
            father = o.optString("father"),
            phone = o.optString("phone"),
            tazkira = o.optString("tazkira"),
            province = o.optString("province"),
            district = o.optString("district"),
            date = o.optString("date"),
            time = o.optString("time"),
            department = o.optString("department"),
            subject = o.optString("subject"),
            responsible = o.optString("responsible"),
            photo = o.optString("photo"),
            attachment = o.optString("attachment"),
            note = o.optString("note")
        )
    } catch (_: Exception) {
        null
    }
}

fun saveVisitors(
    c: Context,
    visitors: List<Visitor>
) {
    val array = JSONArray()

    visitors.forEach {
        array.put(JSONObject(encode(it)))
    }

    prefs(c)
        .edit()
        .putString(DATA_KEY, array.toString())
        .apply()
}

fun loadVisitors(c: Context): List<Visitor> {
    val raw = prefs(c).getString(DATA_KEY, null)
        ?: return emptyList()

    return try {
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                decode(array.getJSONObject(i).toString())?.let(::add)
            }
        }
    } catch (_: Exception) {
        // د پخوانۍ نسخې د newline storage لپاره compatibility
        raw.lines()
            .filter { it.isNotBlank() }
            .mapNotNull { oldDecode(it) }
    }
}

private fun oldUnescape(s: String): String =
    s.replace("\\p", "|").replace("\\\\", "\\")

private fun oldDecode(s: String): Visitor? {
    return try {
        val a = s.split("|")
        if (a.size < 15) return null

        Visitor(
            id = a[0].toInt(),
            name = oldUnescape(a[1]),
            father = oldUnescape(a[2]),
            phone = oldUnescape(a[3]),
            tazkira = oldUnescape(a[4]),
            province = oldUnescape(a[5]),
            district = oldUnescape(a[6]),
            date = oldUnescape(a[7]),
            time = oldUnescape(a[8]),
            department = oldUnescape(a[9]),
            subject = oldUnescape(a[10]),
            responsible = oldUnescape(a[11]),
            photo = oldUnescape(a[12]),
            attachment = oldUnescape(a[13]),
            note = oldUnescape(a[14])
        )
    } catch (_: Exception) {
        null
    }
}

fun exportCsv(
    c: Context,
    v: List<Visitor>
) {
    val dir = c.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)

    if (dir == null) {
        Toast.makeText(c, "د فایلونو فولډر پیدا نه شو.", Toast.LENGTH_LONG).show()
        return
    }

    dir.mkdirs()

    val f = File(
        dir,
        "mراجعین_${System.currentTimeMillis()}.csv"
    )

    val header =
        "ثبت نمبر,نوم,پلار نوم,موبایل,تذکره,ولایت,ولسوالي,نېټه,وخت,اداره,موضوع,مسئول,یادښت"

    val rows = v.joinToString("\n") {
        listOf(
            it.id.toString(),
            it.name,
            it.father,
            it.phone,
            it.tazkira,
            it.province,
            it.district,
            it.date,
            it.time,
            it.department,
            it.subject,
            it.responsible,
            it.note
        ).joinToString(",") { value ->
            "\"${value.replace("\"", "\"\"")}\""
        }
    }

    f.writeText(
        "$header\n$rows",
        Charsets.UTF_8
    )

    Toast.makeText(
        c,
        "Excel فایل جوړ شو: ${f.name}",
        Toast.LENGTH_LONG
    ).show()
}

fun exportPdf(
    c: Context,
    v: List<Visitor>
) {
    val pdf = android.graphics.pdf.PdfDocument()
    var pageNumber = 1
    var y = 60f

    var page = pdf.startPage(
        android.graphics.pdf.PdfDocument.PageInfo
            .Builder(595, 842, pageNumber)
            .create()
    )

    var canvas = page.canvas

    val paint = android.graphics.Paint().apply {
        textSize = 12f
    }

    canvas.drawText(
        "Kunduz Mines - Visitors Report",
        40f,
        35f,
        paint
    )

    v.forEach {
        if (y > 800f) {
            pdf.finishPage(page)
            pageNumber++

            page = pdf.startPage(
                android.graphics.pdf.PdfDocument.PageInfo
                    .Builder(595, 842, pageNumber)
                    .create()
            )

            canvas = page.canvas
            y = 40f
        }

        canvas.drawText(
            "${it.id} | ${it.name} | ${it.phone} | ${it.date} | ${it.subject}",
            30f,
            y,
            paint
        )

        y += 20f
    }

    pdf.finishPage(page)

    val dir =
        c.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)

    if (dir == null) {
        pdf.close()
        Toast.makeText(
            c,
            "د فایلونو فولډر پیدا نه شو.",
            Toast.LENGTH_LONG
        ).show()
        return
    }

    dir.mkdirs()

    val f = File(
        dir,
        "visitors_${System.currentTimeMillis()}.pdf"
    )

    f.outputStream().use {
        pdf.writeTo(it)
    }

    pdf.close()

    Toast.makeText(
        c,
        "PDF جوړ شو: ${f.name}",
        Toast.LENGTH_LONG
    ).show()
}

fun backup(
    c: Context,
    v: List<Visitor>
) {
    val dir =
        c.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)

    if (dir == null) {
        Toast.makeText(
            c,
            "د فایلونو فولډر پیدا نه شو.",
            Toast.LENGTH_LONG
        ).show()
        return
    }

    dir.mkdirs()

    val f = File(
        dir,
        "backup_${System.currentTimeMillis()}.json"
    )

    val array = JSONArray()
    v.forEach {
        array.put(JSONObject(encode(it)))
    }

    f.writeText(
        array.toString(2),
        Charsets.UTF_8
    )

    Toast.makeText(
        c,
        "Backup جوړ شو: ${f.name}",
        Toast.LENGTH_LONG
    ).show()
}
