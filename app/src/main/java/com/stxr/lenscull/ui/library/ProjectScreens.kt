package com.stxr.lenscull.ui.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.stxr.lenscull.domain.CullProject
import com.stxr.lenscull.domain.ProjectSourceType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectHomeScreen(
  projects: List<CullProject>,
  onCreate: (String) -> Unit,
  onOpen: (CullProject) -> Unit,
  onDelete: (CullProject) -> Unit,
) {
  var creating by remember { mutableStateOf(false) }
  var projectName by remember { mutableStateOf("") }
  Scaffold(
    topBar = { TopAppBar(title = { Text("LensCull 项目") }) },
    floatingActionButton = {
      FloatingActionButton(onClick = { creating = true }) {
        Icon(Icons.Rounded.Add, "新建项目")
      }
    },
  ) { padding ->
    if (projects.isEmpty()) {
      Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Rounded.PhotoLibrary, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
          Text("先创建一个选片项目", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 16.dp))
          Text("每个项目独立管理目录和入选照片", color = MaterialTheme.colorScheme.onSurfaceVariant)
          Button(onClick = { creating = true }, modifier = Modifier.padding(top = 20.dp)) { Text("创建项目") }
        }
      }
    } else {
      LazyColumn(
        Modifier.fillMaxSize().padding(padding).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        items(projects, key = { it.id }) { project ->
          Card(Modifier.fillMaxWidth().clickable { onOpen(project) }) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
              Icon(if (project.sourceType == ProjectSourceType.ALL_STORAGE) Icons.Rounded.Storage else Icons.Rounded.Folder, null)
              Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(project.name, style = MaterialTheme.typography.titleMedium)
                Text(
                  when (project.sourceType) {
                    ProjectSourceType.UNCONFIGURED -> "尚未选择照片来源"
                    ProjectSourceType.ALL_STORAGE -> "扫描全部存储"
                    ProjectSourceType.DIRECTORY -> project.sourcePath ?: "指定目录"
                  },
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              IconButton(onClick = { onDelete(project) }) { Icon(Icons.Rounded.Delete, "删除 ${project.name}") }
            }
          }
        }
      }
    }
  }

  if (creating) {
    AlertDialog(
      onDismissRequest = { creating = false },
      title = { Text("创建选片项目") },
      text = {
        OutlinedTextField(
          value = projectName,
          onValueChange = { projectName = it },
          label = { Text("项目名称") },
          singleLine = true,
        )
      },
      confirmButton = {
        Button(
          enabled = projectName.isNotBlank(),
          onClick = { onCreate(projectName); projectName = ""; creating = false },
        ) { Text("创建") }
      },
      dismissButton = { TextButton(onClick = { creating = false }) { Text("取消") } },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectSetupScreen(
  project: CullProject,
  onBack: () -> Unit,
  onAllStorage: () -> Unit,
  onDirectory: (android.net.Uri) -> Unit,
) {
  val context = LocalContext.current
  val directoryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
    uri?.let {
      runCatching {
        context.contentResolver.takePersistableUriPermission(
          it,
          android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
      }
      onDirectory(it)
    }
  }
  Scaffold(topBar = { TopAppBar(title = { Text(project.name) }) }) { padding ->
    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
      Column(Modifier.widthIn(max = 560.dp).padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("选择项目照片来源", style = MaterialTheme.typography.headlineSmall)
        Text(
          "可以扫描设备中的所有照片，也可以只扫描一个拍摄目录。之后仍可在项目内按格式、星级和标记筛选。",
          modifier = Modifier.padding(vertical = 18.dp),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onAllStorage, modifier = Modifier.fillMaxWidth()) {
          Icon(Icons.Rounded.Storage, null)
          Spacer(Modifier.size(8.dp))
          Text("扫描全部存储")
        }
        OutlinedButton(onClick = { directoryLauncher.launch(null) }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
          Icon(Icons.Rounded.Folder, null)
          Spacer(Modifier.size(8.dp))
          Text("选择指定目录")
        }
        TextButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) { Text("返回项目列表") }
      }
    }
  }
}
