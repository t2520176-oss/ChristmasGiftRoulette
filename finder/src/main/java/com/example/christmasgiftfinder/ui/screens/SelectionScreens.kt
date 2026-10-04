package com.example.christmasgiftfinder.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.christmasgiftfinder.R
import com.example.christmasgiftfinder.data.Countries
import com.example.christmasgiftfinder.model.Country
import com.example.christmasgiftfinder.ui.components.ContentMaxWidth
import com.example.christmasgiftfinder.ui.components.ScreenScaffold
import com.example.christmasgiftfinder.ui.theme.InkSoft
import com.example.christmasgiftfinder.ui.theme.Snow

private val SelectedRowColor = Color(0xFFFFE9E6)

@Composable
fun CountryScreen(selected: Country?, onSelect: (Country) -> Unit, onBack: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val countries = Countries.search(query)

    ScreenScaffold(stringResource(R.string.select_country_title), onBack) {
        Column(Modifier.widthIn(max = ContentMaxWidth).fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text(stringResource(R.string.search_country)) },
            )
            if (countries.isEmpty()) {
                Text(
                    stringResource(R.string.no_country_found),
                    modifier = Modifier.padding(24.dp),
                    color = InkSoft,
                    fontSize = 16.sp,
                )
            }
            LazyColumn(
                Modifier.fillMaxSize().navigationBarsPadding(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(countries, key = { it.name }) { country ->
                    val isSelected = country.name == selected?.name
                    OptionRow(
                        icon = country.flag,
                        label = country.name,
                        trailing = "${country.currencyCode} (${country.currencySymbol})",
                        selected = isSelected,
                        onClick = { onSelect(country) },
                    )
                }
            }
        }
    }
}

/** One entry of a simple single-choice list (recipient / gift style). */
data class Option(val key: String, val icon: String, val label: String)

@Composable
fun OptionListScreen(
    title: String,
    options: List<Option>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    onBack: () -> Unit,
) {
    ScreenScaffold(title, onBack) {
        LazyColumn(
            Modifier.widthIn(max = ContentMaxWidth).fillMaxSize().navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(options, key = { it.key }) { option ->
                OptionRow(
                    icon = option.icon,
                    label = option.label,
                    selected = option.key == selectedKey,
                    onClick = { onSelect(option.key) },
                )
            }
        }
    }
}

@Composable
private fun OptionRow(icon: String, label: String, selected: Boolean, onClick: () -> Unit, trailing: String? = null) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) SelectedRowColor else Snow,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 26.sp)
            Spacer(Modifier.size(14.dp))
            Text(label, modifier = Modifier.weight(1f), fontSize = 18.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
            if (trailing != null) {
                Text(trailing, fontSize = 15.sp, color = InkSoft)
                Spacer(Modifier.size(8.dp))
            }
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
