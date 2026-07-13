# Releasing BLOKD

Release APKs are built and signed by CI (`.github/workflows/release.yml`) when a `v*` tag is pushed. Set up signing once, then cut releases by tagging.

## One-time: create a signing keystore

```
keytool -genkeypair -v \
  -keystore blokd.jks \
  -alias blokd \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass "YOUR_STORE_PASSWORD" \
  -keypass "YOUR_KEY_PASSWORD" \
  -dname "CN=anishfyi, O=BLOKD"
```

Keep `blokd.jks` and both passwords safe and out of git. The same key must sign every release, or users cannot update in place.

## One-time: add repository secrets

```
base64 -i blokd.jks | tr -d '\n' | pbcopy   # copies the encoded keystore

gh secret set SIGNING_KEYSTORE_BASE64        # paste the clipboard value
gh secret set SIGNING_KEYSTORE_PASSWORD      # YOUR_STORE_PASSWORD
gh secret set SIGNING_KEY_ALIAS              # blokd
gh secret set SIGNING_KEY_PASSWORD           # YOUR_KEY_PASSWORD
```

## Cut a release

```
git tag v0.1.0
git push origin v0.1.0
```

CI builds `:app:assembleRelease`, signs it, and attaches `BLOKD-v0.1.0.apk` to a new GitHub release with generated notes.

## Enable the site

Repository Settings, Pages, Source: `main` branch, `/docs` folder. The site publishes at https://anishfyi.github.io/BLOKD/.
