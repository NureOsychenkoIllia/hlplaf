package main

import (
	_ "embed"
	"fmt"
	"log/slog"
	"net/http"

	"github.com/go-chi/chi/v5"
	chimid "github.com/go-chi/chi/v5/middleware"
)

//go:embed openapi.yaml
var specBytes []byte

func main() {
	db := initDB("social.db")

	r := chi.NewRouter()
	r.Use(chimid.Recoverer)
	r.Use(chimid.RealIP)

	// Scalar API explorer
	r.Get("/", scalarPage)
	r.Get("/openapi.yaml", serveSpec)

	// Public auth endpoints
	r.Post("/auth/register", register(db))
	r.Post("/auth/login", login(db))

	// Authenticated routes — queryLogger wraps jwtAuth so it can capture userID
	r.Group(func(r chi.Router) {
		r.Use(queryLogger(db)) // L4: request logging (outer — captures userID set by jwtAuth)
		r.Use(jwtAuth)         // L4: JWT validation

		// Posts (L2: CRUD, L3: search & filter)
		r.Get("/posts", listPosts(db))
		r.Post("/posts", createPost(db))
		r.Get("/posts/{id}", getPost(db))
		r.Put("/posts/{id}", updatePost(db))
		r.Delete("/posts/{id}", deletePost(db))

		// Comments (L2)
		r.Get("/posts/{id}/comments", listComments(db))
		r.Post("/posts/{id}/comments", createComment(db))

		// Likes
		r.Post("/posts/{id}/like", likePost(db))
		r.Delete("/posts/{id}/like", unlikePost(db))

		// Friends (L1: many-to-many)
		r.Get("/me/friends", listFriends(db))
		r.Post("/me/friends/{fid}", addFriend(db))
		r.Delete("/me/friends/{fid}", removeFriend(db))

		// Admin logs (L4: monitoring)
		r.Get("/admin/logs", adminLogs(db))
	})

	slog.Info("server started", "addr", "http://localhost:8080")
	if err := http.ListenAndServe(":8080", r); err != nil {
		slog.Error("server error", "err", err)
	}
}

func scalarPage(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "text/html; charset=utf-8")
	fmt.Fprint(w, `<!doctype html>
<html>
<head>
  <title>SocialNet API</title>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
</head>
<body>
  <script id="api-reference" data-url="/openapi.yaml"></script>
  <script src="https://cdn.jsdelivr.net/npm/@scalar/api-reference"></script>
</body>
</html>`)
}

func serveSpec(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/yaml")
	w.Write(specBytes)
}
