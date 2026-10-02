# MultiCardStack

Compose Multiplatform (Android / iOS) 向けの、Tinder 風スワイプカード UI ライブラリです。

- 上下左右スワイプ（方向ごとに有効/無効を指定可能）。ドラッグ量だけでなく、フリックの速度でも判定
- ドラッグに合わせてカードが傾き、背面のカードがせり上がってくるアニメーション
- `LIKE` / `NOPE` などのオーバーレイ用スロット（進捗 `0f..1f` 付き）
- `CardStackState` 経由でボタンから `swipe()` / `undo()` / `reset()`
- 画面回転やプロセス再生成をまたいで、位置とスワイプ履歴を保持

## 使い方

```kotlin
val state = rememberCardStackState()
val scope = rememberCoroutineScope()

CardStack(
    items = profiles,
    state = state,
    directions = SwipeDirection.All,          // 既定は SwipeDirection.Horizontal
    key = { it.id },
    onSwiped = { profile, direction -> /* ... */ },
    onUndo = { profile, direction -> /* ... */ },
    overlay = { _, direction, progress ->
        Text(
            if (direction == SwipeDirection.Right) "LIKE" else "NOPE",
            Modifier.align(Alignment.TopCenter).graphicsLayer { alpha = progress },
        )
    },
    modifier = Modifier.fillMaxSize(),
) { profile ->
    ProfileCard(profile)
}

Button(onClick = { scope.launch { state.swipe(SwipeDirection.Right) } }) { Text("Like") }
Button(onClick = { scope.launch { state.undo() } }, enabled = state.canUndo) { Text("Undo") }
```

すべてのカードをスワイプし終えると `state.currentIndex == items.size` になり、何も描画されなくなります。空状態の表示は利用側で行ってください。`items` に要素を追加すれば、そのままスタックの続きとして表示されます。

### 主なパラメータ

| パラメータ | 既定値 | 説明 |
| --- | --- | --- |
| `directions` | `Horizontal` | ジェスチャーでスワイプできる方向。`state.swipe()` はこの設定に関係なく全方向に使える |
| `visibleCount` | `3` | 同時に描画するカードの枚数（一番上のカードを含む） |
| `swipeThreshold` | `0.3f` | スワイプが成立するドラッグ量（カードサイズに対する割合） |
| `velocityThreshold` | `1000.dp` | この速度（/秒）以上のフリックなら、距離が足りなくてもスワイプ成立 |
| `maxRotation` | `15f` | 横にカード1枚分ドラッグしたときの傾き（度） |
| `stackScaleStep` / `stackOffset` | `0.04f` / `10.dp` | 背面カードの縮小率と、下からのはみ出し量 |

カードの描画領域は `CardStack` のサイズから、背面カードがはみ出す分（`stackOffset × (visibleCount - 1)`）を下側に差し引いたサイズになります。

## 構成

```
multicardstack/        ライブラリ本体 (commonMain)
sample/shared/         サンプル UI (Compose Multiplatform、iOS 向け framework "SampleShared")
sample/androidApp/     Android サンプルアプリ
sample/iosApp/         iOS サンプルアプリ (Xcode プロジェクト)
```

## ビルドと実行

JDK 17 以上が必要です（Android Studio 同梱の JBR で動作します）。

```sh
./gradlew :multicardstack:allTests            # ユニットテスト + iOS シミュレータ上の UI テスト
./gradlew :sample:androidApp:installDebug     # Android サンプルを端末にインストール
open sample/iosApp/iosApp.xcodeproj           # iOS サンプル (Xcode から Run)
```

iOS のビルドフェーズで `./gradlew :sample:shared:embedAndSignAppleFrameworkForXcode` が実行されます。実機で動かす場合は、Xcode の Signing & Capabilities で Team を設定してください。
