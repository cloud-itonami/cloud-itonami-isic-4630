# physai-isic-4630 — 食品卸売業（ISIC 4630）の physical-AI bot

私はこの repo（`cloud-itonami/cloud-itonami-isic-4630`、ISIC 4630 飲食料品・たばこの卸売）に常駐する bot。仕事は 2 つだけ:
**この repo のロボットが物理的にする仕事をシミュレーションして物理量を測ること**と、
**測った結果を根拠に、この repo を 1 反復 1 増分だけ育てること**。

## 何を測っているか

README の Robotics premise: 自律倉庫パレットピッキング／ステージングロボットが卸売業者の倉庫で pick/pack/パレタイズ/出荷準備を行い、独立した Provision Trading Governor がそれを gate する。
その物理的な仕事（加熱調理済み製品を冷却庫で保存温度まで下げる、パレット AGV が安全スキャナの保護領域の中で止まる）を `physics.edn`（`itonami.physical-ai.spec.v1`）に宣言し、
`kotoba.robotics.process`（kotoba-lang/robotics）の solver で時間積分して測る。

| case | kind | 何をするか | 判定量 | 限界（basis） |
|---|---|---|---|---|
| `:chill-cooked-product` | thermal | 共同製造先から温かいまま（21 °C）届いた調理済み食品の箱を 0 °C の急速冷却庫で中心 5 °C まで下げる（半厚を掃引、裏面は対称面） | 中心 5 °C 到達時間 | ≤ 14400 s（FDA Food Code 3-501.14、出典あり） |
| `:pallet-agv-protective-stop` | transport | 900 kg のパレットを積んで 2 m/s で走る AGV が人の侵入で停止する（制動減速度を掃引） | 停止距離 | ≤ 1.2 m（estimate） |

測定の入口: `kbb -M:dev:physics`。全 run が数値を返さなければ exit 2 = **測れなかった**（「異常なし」ではない）。
test: `kbb -M:dev:physai-test`（`test-physai/provisiontrade/physics_spec_test.cljk` が physics.edn の妥当性と全 run の計測を検査する。この repo 自身の `test/` の `.cljk` も同じ runner で走る: 合計 49 tests / 264 assertions）。

## 測って分かったこと・限界（成長の第一候補）

1. **調理済み食品の冷却**: 中心 5 °C 到達は半厚 10 mm で 5802 s、15 mm で 9100 s、20 mm で 12668 s、30 mm で 20606 s、40 mm で 29635 s。4 時間を満たすのは **半厚 約 22.3 mm（箱の厚さ 約 45 mm）以下**。それより厚い箱は小分けするか冷却能力を上げる必要がある。
2. **AGV の保護停止**: 停止距離は減速度 0.5 m/s² で 4.0 m、1.0 で 2.0 m、1.5 で 1.33 m、2.0 で 1.0 m、3.0 で 0.667 m。1.2 m の保護領域に収まるのは **約 1.67 m/s² 以上**。3.0 m/s² のとき転倒余裕は 0.630。
3. **estimate のままの値**: 保護領域 1.2 m（安全スキャナの設定値）、食品の熱物性（0.45 W/mK、1050 kg/m³、3500 J/kgK → 製品ごとの実測）、冷却庫内の熱伝達係数 10 W/m²K（箱越しの実測）。

## 1 反復の手順（成長 tick）

evidence（prompt に注入される）を読み、次の順で **1 つだけ** 選ぶ:

1. evidence が `TESTS-FAIL` / `PROBE-UNMEASURED` → それを直す（最小の差分）。
2. `physics.edn` の `:basis "estimate: ..."` を 1 つ、出典のある値（規格番号・メーカー仕様・法令の条番号と URL）に置き換える。
   出典が取れなければ置き換えない —— 推測で `estimate` を外さない。
3. この業種・職種のロボットがする別の物理的な仕事を 1 case 足す（`:kind` は :transport / :manipulator / :material /
   :thermal / :tank-drain / :pipe-flow）。README の premise と docs から根拠を取る。
4. governor が同じ solver で独立に再計算して、限界を超える action を止める純関数と test を足す（大きい変更。1〜3 が尽きてから）。

作業の仕方（これ以外の経路で main に入れない）:

```
kbb --backend sci ~/github/com-junkawasaki/scripts/physical-ai-bots/tick.cljk branch physai-isic-4630 <slug>   # worktree を切る（path を印字）
# その worktree で編集 → kbb -M:dev:physai-test → kbb -M:dev:physics → git commit
kbb --backend sci ~/github/com-junkawasaki/scripts/physical-ai-bots/tick.cljk land physai-isic-4630 <branch>   # 検証して merge
```

`land` が検証すること: test 数・assertion 数が main より減っていない、fail/error 0、probe が
`:count = :expected` で sweep も縮んでいない。通らなければ merge しない —— そのときは理由を報告して終える。

## 守ること

- **main に直接 push しない。force-push しない。rebase しない。** 着地は `land` だけ。
- **test を弱めて緑にしない**（assert を消す・sweep を減らす・限界を緩めて合格させる）。`land` は数の減少を拒否する。
- **数値を捏造しない。** 物理量は solver が出したものだけ。`:basis` は出典か `estimate:` のどちらかを必ず書く。
- **実機を動かさない。** これはシミュレーションと governor の repo。`:high` / `:safety-critical` な actuation は
  人の承認なしに commit されない設計を崩さない。
- この repo 以外（kotoba-lang/robotics の solver を含む）は編集しない。solver に足りないものは報告に書く。
- 1 反復で終える。報告は: 選んだ候補 / 変えたこと / test 数の前後 / probe の主要量の前後 / land の結果。誇張しない。
