// ============================================================
// Nazwa pakietu. Określa, gdzie w projekcie leży ta klasa.
// Musi się zgadzać z folderem w app/src/main/java/... oraz z namespace w build.gradle.
// ============================================================
package com.example.myapplication;

// ------------------------------------------------------------
// IMPORTY: mówimy kompilatorowi, z jakich klas Androida korzystamy.
// Bez importu klasa byłaby podkreślona na czerwono (Cannot resolve symbol).
// ------------------------------------------------------------
import android.graphics.Color;                    // narzędzia do kolorów: parseColor(), alpha()
import android.os.Bundle;                         // paczka z zapisanym stanem aktywności (onCreate)
import android.text.Editable;                     // edytowalny tekst z pola EditText (parametr afterTextChanged)
import android.text.TextWatcher;                  // interfejs "obserwatora" zmian tekstu
import android.widget.EditText;                   // pole do wpisywania tekstu
import android.widget.LinearLayout;               // typ naszego panelu
import android.widget.RadioGroup;                 // grupa przycisków radiowych
import android.widget.SeekBar;                    // suwak
import android.widget.TextView;                   // napis

import androidx.appcompat.app.AppCompatActivity;  // bazowa klasa ekranu (zgodna ze starszymi Androidami)
import androidx.core.graphics.ColorUtils;         // setAlphaComponent(): podmiana kanału alfa w kolorze

// ============================================================
// KLASA EKRANU
// "extends AppCompatActivity" oznacza, że MainActivity jest jednym ekranem aplikacji
// i dziedziczy po AppCompatActivity cały cykl życia (onCreate, onStart, ...).
// ============================================================
public class MainActivity extends AppCompatActivity {

    // --------------------------------------------------------
    // KOLOR BAZOWY
    // static final = stała wspólna dla całej klasy, której nie można zmienić.
    // Color.parseColor("#CC121212") zamienia tekst #AARRGGBB na liczbę int (format ARGB):
    //   CC = alfa 204 (około 80% krycia), 12 12 12 = ciemnoszary.
    // To ten sam kolor, który ustawiliśmy w XML w android:background panelu.
    // Od niego liczymy procenty: 100% = alfa 204, 50% = alfa 102, 0% = alfa 0.
    // --------------------------------------------------------
    private static final int BASE_COLOR = Color.parseColor("#CC121212");

    // --------------------------------------------------------
    // POLA KLASY: referencje do widoków z layoutu.
    // Deklarujemy je tu (jako pola), a nie wewnątrz onCreate, bo korzystamy z nich także
    // w innych metodach (np. applyAlpha) i w listenerach.
    // Wartości dostaną dopiero w onCreate przez findViewById.
    // --------------------------------------------------------
    private LinearLayout panel;     // panel, którego tło robimy przezroczystym
    private TextView tvInfo;        // napis "Przezroczystość panelu: X%"
    private RadioGroup radioGroup;  // grupa przycisków 100/75/50/25%
    private EditText etAlpha;       // pole do wpisania liczby
    private SeekBar seekAlpha;      // suwak 0-100

    // --------------------------------------------------------
    // FLAGA "updating": ochrona przed nieskończoną pętlą.
    // Problem: zmieniamy wartość z kodu (np. seekAlpha.setProgress), a to wywołuje listener suwaka,
    // który znów zmienia inne kontrolki, a one wywołują swoje listenery i tak w kółko.
    // Rozwiązanie: na czas aktualizacji ustawiamy updating = true,
    // a każdy listener na samym początku sprawdza flagę i jeśli jest true, od razu kończy pracę.
    // Dzięki temu listenery reagują tylko na działania UŻYTKOWNIKA, a nie na zmiany z kodu.
    // --------------------------------------------------------
    private boolean updating = false;

    // ============================================================
    // onCreate: pierwsza metoda cyklu życia, wywoływana przy tworzeniu ekranu.
    // Tu podłączamy layout, szukamy widoków i ustawiamy listenery.
    // @Override = nadpisujemy metodę z klasy nadrzędnej.
    // protected = metoda dostępna dla klasy i jej potomków.
    // ============================================================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Wywołanie metody z klasy nadrzędnej jest obowiązkowe.
        // Bez niego aplikacja wyrzuci wyjątek SuperNotCalledException.
        super.onCreate(savedInstanceState);

        // Ładuje layout res/layout/activity_main.xml i zamienia go na obiekty widoków.
        // Dopiero po tej linii findViewById może coś znaleźć.
        setContentView(R.layout.activity_main);

        // Wiążemy pola z widokami z XML po ich ID (android:id="@+id/...").
        // Typ zwracany jest dobierany automatycznie do typu pola po lewej stronie.
        panel = findViewById(R.id.panel);
        tvInfo = findViewById(R.id.tvInfo);
        radioGroup = findViewById(R.id.radioGroup);
        etAlpha = findViewById(R.id.etAlpha);
        seekAlpha = findViewById(R.id.seekAlpha);

        // ========================================================
        // SPOSÓB 1: RadioButton
        // ========================================================
        // setOnCheckedChangeListener: wywoływany, gdy zmieni się zaznaczony przycisk w grupie.
        // Zapis (group, checkedId) -> { ... } to wyrażenie lambda (skrócony zapis klasy anonimowej).
        //   group     = grupa, w której nastąpiła zmiana (tu nie używamy)
        //   checkedId = ID zaznaczonego RadioButtona (np. R.id.rb50) albo -1, gdy nic nie zaznaczono
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            // Zmiana pochodzi z kodu (applyAlpha), a nie od użytkownika: ignorujemy.
            if (updating) return;

            // Domyślnie 100%, a potem sprawdzamy, który przycisk wybrano.
            int percent = 100;
            if (checkedId == R.id.rb75) percent = 75;
            else if (checkedId == R.id.rb50) percent = 50;
            else if (checkedId == R.id.rb25) percent = 25;

            // Parametry: percent, setPanel=true, setEdit=true, setSeek=true.
            // Po kliknięciu radio aktualizujemy panel, pole tekstowe i suwak.
            applyAlpha(percent, true, true, true);
        });

        // ========================================================
        // SPOSÓB 2: pole do wpisania
        // ========================================================
        // TextWatcher reaguje na zmiany tekstu w polu. Tworzymy klasę anonimową
        // (new TextWatcher() { ... }), bo interfejs wymaga zaimplementowania trzech metod.
        etAlpha.addTextChangedListener(new TextWatcher() {

            // 1) Wywoływana PRZED zmianą tekstu. Nie potrzebujemy jej, więc jest pusta,
            //    ale interfejs wymaga, żeby była.
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            // 2) Wywoływana W TRAKCIE zmiany tekstu. Też niepotrzebna, więc pusta.
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }

            // 3) Wywoływana PO zmianie tekstu. Tu odczytujemy gotową wartość.
            //    Parametr s to aktualna zawartość pola.
            @Override
            public void afterTextChanged(Editable s) {
                // Ignorujemy zmiany wywołane przez nasz własny kod (etAlpha.setText w applyAlpha).
                if (updating) return;

                // toString() zamienia Editable na zwykły String, trim() obcina spacje na brzegach.
                String text = s.toString().trim();

                // Użytkownik skasował wszystko: nie ma czego parsować, więc nic nie zmieniamy.
                // (Bez tego parseInt("") rzuciłby NumberFormatException.)
                if (text.isEmpty()) return;

                int percent;
                try {
                    // Zamiana tekstu na liczbę całkowitą.
                    percent = Integer.parseInt(text);
                } catch (NumberFormatException e) {
                    // Zabezpieczenie na wypadek tekstu, którego nie da się zamienić na liczbę.
                    // inputType="number" już to utrudnia, ale lepiej mieć dodatkową ochronę.
                    return;
                }

                // Ograniczamy wartość do zakresu 0-100:
                //   Math.min(100, percent) = nie więcej niż 100 (np. 250 daje 100)
                //   Math.max(0, ...)       = nie mniej niż 0
                percent = Math.max(0, Math.min(100, percent));

                // setEdit=false: NIE nadpisujemy pola tekstowego, bo użytkownik właśnie w nim pisze.
                // Gdybyśmy zrobili setText, kursor skakałby, a wpisywanie byłoby uciążliwe.
                applyAlpha(percent, true, false, true);
            }
        });

        // ========================================================
        // SPOSÓB 3: suwak
        // ========================================================
        // OnSeekBarChangeListener wymaga trzech metod (jak w Twojej lekcji).
        seekAlpha.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            // Wywoływana przy każdej zmianie położenia suwaka.
            //   seekBar  = suwak, który się zmienił
            //   progress = bieżąca wartość 0-100 (to od razu nasze procenty, bo android:max="100")
            //   fromUser = true, gdy ruszył nim palec, a false, gdy zmiana pochodzi z kodu
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Ignorujemy zmiany z kodu (setProgress w applyAlpha).
                if (updating) return;

                // setSeek=false: suwak sam jest źródłem zmiany, więc nie ustawiamy go ponownie.
                applyAlpha(progress, true, true, false);
            }

            // Początek przeciągania suwaka: niepotrzebne, więc puste.
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }

            // Koniec przeciągania suwaka: niepotrzebne, więc puste.
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });
    }

    // ============================================================
    // applyAlpha: jedna metoda, która robi całą robotę.
    // Wszystkie trzy sposoby wywołują ją, więc logika jest w jednym miejscu (bez kopiowania kodu).
    // Cztery parametry typu boolean decydują, które kontrolki mamy zaktualizować.
    // Dlaczego nie aktualizujemy zawsze wszystkiego? Bo kontrolka, od której wyszła zmiana,
    // nie powinna być ustawiana ponownie (np. pole, w którym ktoś pisze).
    //
    //   percent  = 0-100 (0 = niewidoczny, 100 = alfa z koloru bazowego)
    //   setPanel = czy zmienić tło panelu
    //   setEdit  = czy zaktualizować pole tekstowe
    //   setSeek  = czy zaktualizować suwak
    // ============================================================
    private void applyAlpha(int percent, boolean setPanel, boolean setEdit, boolean setSeek) {
        // Od tej chwili zmiany pochodzą z kodu, więc listenery mają je ignorować.
        updating = true;

        // try/finally gwarantuje, że flaga wróci do false NAWET wtedy, gdy w środku poleci wyjątek.
        // Bez tego wyjątek zostawiłby updating = true i kontrolki przestałyby reagować na dotyk.
        try {
            // ---- Obliczenie alfy ----
            // Color.alpha(BASE_COLOR) wyciąga kanał alfa z koloru bazowego (tu 204).
            // Skalujemy go przez percent / 100:
            //   100% daje 204, 50% daje 102, 0% daje 0.
            // KOLEJNOŚĆ MA ZNACZENIE: najpierw mnożymy, potem dzielimy.
            // Zapis percent / 100 * 204 byłby błędny, bo dzielenie liczb całkowitych
            // dałoby 0 (dla percent poniżej 100) i panel zawsze by znikał.
            // Wynik zawsze mieści się w 0-255, więc setAlphaComponent nie rzuci wyjątku.
            int alpha = Color.alpha(BASE_COLOR) * percent / 100;

            // ---- Panel ----
            if (setPanel) {
                // setAlphaComponent zwraca ten sam kolor (R, G, B bez zmian) z nową alfą.
                // setBackgroundColor ustawia go jako tło panelu.
                // Przezroczyste jest tylko TŁO: teksty, przyciski i suwak w panelu zostają w pełni widoczne.
                // (Gdybyśmy użyli panel.setAlpha(...), zblakłaby cała zawartość.)
                panel.setBackgroundColor(ColorUtils.setAlphaComponent(BASE_COLOR, alpha));
            }

            // ---- Pole tekstowe ----
            if (setEdit) {
                // String.valueOf zamienia liczbę na tekst. setText oczekuje tekstu, a nie liczby:
                // przekazanie samego int zostałoby potraktowane jako ID zasobu i aplikacja by się wywaliła.
                etAlpha.setText(String.valueOf(percent));

                // setText przenosi kursor na początek, więc przesuwamy go na koniec tekstu,
                // żeby wygodnie było dalej pisać.
                etAlpha.setSelection(etAlpha.getText().length());
            }

            // ---- Suwak ----
            if (setSeek) {
                // Ustawia pozycję suwaka. Ponieważ android:max="100", wartość jest wprost procentem.
                seekAlpha.setProgress(percent);
            }

            // ---- Radiobuttony ----
            // Zaznaczamy przycisk pasujący do wartości. To też synchronizuje grupę, gdy
            // zmiana przyszła z suwaka lub pola tekstowego.
            // Gdy wartość nie pasuje do żadnego przycisku (np. 37%), clearCheck()
            // odznacza wszystkie, żeby nie sugerować błędnej wartości.
            // check() i clearCheck() wywołują listener radiogrupy, ale flaga updating
            // go zablokuje.
            if (percent == 100) radioGroup.check(R.id.rb100);
            else if (percent == 75) radioGroup.check(R.id.rb75);
            else if (percent == 50) radioGroup.check(R.id.rb50);
            else if (percent == 25) radioGroup.check(R.id.rb25);
            else radioGroup.clearCheck();

            // ---- Napis z wartością ----
            // Sklejamy tekst z liczbą. W prawdziwej aplikacji lepiej użyć
            // getString(R.string.alpha_panel_label, percent) ze strings.xml (jak w lekcji).
            tvInfo.setText("Przezroczystość panelu: " + percent + "%");

        } finally {
            // Koniec aktualizacji: listenery znów reagują na działania użytkownika.
            updating = false;
        }
    }
}