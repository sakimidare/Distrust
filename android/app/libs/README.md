# Go core AAR

Place a locally built `zju-connect.aar` in this directory to enable the current
EasyConnect proof-of-concept backend. The AAR itself is intentionally ignored by
Git so releases remain reproducible from an explicitly pinned upstream source.

The current upstream mobile API does **not** support aTrust or local SOCKS5/HTTP
proxy mode yet. Distrust detects this at runtime and reports the missing
capability instead of pretending a connection succeeded.
