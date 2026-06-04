#!/usr/bin/env python3
"""
RAG index for Tarnish codebase — semantic code search via FAISS.
Usage:
  python3 rag_index.py build       # Build the FAISS index
  python3 rag_index.py query "how to add an item"  # Semantic search
  python3 rag_index.py serve       # Start REST endpoint on port 9191
"""

import json, os, sys, argparse
from pathlib import Path

PROJECT_ROOT = Path(__file__).parent.resolve()
INDEX_FILE = PROJECT_ROOT / "repo.faiss"
META_FILE = PROJECT_ROOT / "repo_meta.json"
CHUNK_SIZE = 200  # tokens per chunk
CHUNK_OVERLAP = 50

def get_chunks(text: str, chunk_tokens: int = CHUNK_SIZE, overlap: int = CHUNK_OVERLAP) -> list[str]:
    """Split text into overlapping chunks."""
    words = text.split()
    chunks = []
    i = 0
    while i < len(words):
        chunk = " ".join(words[i:i + chunk_tokens])
        chunks.append(chunk)
        i += chunk_tokens - overlap
    return chunks

def build_index():
    """Build FAISS index from all source files."""
    try:
        import sentence_transformers
        import faiss
        import numpy as np
    except ImportError:
        print("Install: pip install sentence-transformers faiss-cpu numpy")
        sys.exit(1)

    # Load existing file index
    with open(PROJECT_ROOT / "code_index.json") as f:
        files = json.load(f)

    print(f"Loading {len(files)} files...")
    chunks = []
    meta = []

    for entry in files:
        fpath = PROJECT_ROOT / entry["path"]
        if not fpath.exists():
            continue
        try:
            text = fpath.read_text(encoding="utf-8", errors="ignore")
            # Skip binary/big files
            if len(text) > 1024 * 1024:  # 1MB
                continue
            file_chunks = get_chunks(text)
            for i, chunk in enumerate(file_chunks):
                chunks.append(chunk)
                meta.append({"path": entry["path"], "chunk": i, "summary": entry["summary"]})
        except:
            continue

    print(f"Embedding {len(chunks)} chunks...")
    model = sentence_transformers.SentenceTransformer("all-MiniLM-L6-v2")
    embeddings = model.encode(chunks, show_progress_bar=True, batch_size=64)
    embeddings = np.array(embeddings).astype("float32")

    # Build FAISS index
    dim = embeddings.shape[1]
    index = faiss.IndexFlatIP(dim)  # Inner product (cosine sim on normalized vectors)
    faiss.normalize_L2(embeddings)
    index.add(embeddings)

    faiss.write_index(index, str(INDEX_FILE))
    with open(META_FILE, "w") as f:
        json.dump(meta, f)

    print(f"Index saved: {len(chunks)} chunks, {dim}d vectors -> {INDEX_FILE}")
    print(f"Meta saved: {len(meta)} entries -> {META_FILE}")

def query_index(query: str, top_k: int = 10):
    """Search FAISS index for relevant code chunks."""
    try:
        import sentence_transformers
        import faiss
        import numpy as np
    except ImportError:
        print("Install: pip install sentence-transformers faiss-cpu numpy")
        sys.exit(1)

    if not INDEX_FILE.exists():
        print("Index not found. Run: python3 rag_index.py build")
        sys.exit(1)

    index = faiss.read_index(str(INDEX_FILE))
    with open(META_FILE) as f:
        meta = json.load(f)

    model = sentence_transformers.SentenceTransformer("all-MiniLM-L6-v2")
    q_embedding = model.encode([query]).astype("float32")
    faiss.normalize_L2(q_embedding)

    scores, indices = index.search(q_embedding, top_k)

    print(f"\nTop {top_k} results for: '{query}'\n")
    for score, idx in zip(scores[0], indices[0]):
        if idx < 0 or idx >= len(meta):
            continue
        m = meta[idx]
        print(f"[{score:.3f}] {m['path']} (chunk {m['chunk']}) — {m['summary']}")
        print()

def serve(port: int = 9191):
    """Start REST endpoint for RAG queries."""
    try:
        from flask import Flask, request, jsonify
    except ImportError:
        print("Install: pip install flask")
        sys.exit(1)

    app = Flask(__name__)

    @app.route("/rag/query", methods=["POST"])
    def rag_query():
        data = request.json
        query = data.get("query", "")
        top_k = data.get("top_k", 10)
        # Reuse query_index logic
        import sentence_transformers, faiss, numpy as np
        index = faiss.read_index(str(INDEX_FILE))
        with open(META_FILE) as f:
            meta = json.load(f)
        model = sentence_transformers.SentenceTransformer("all-MiniLM-L6-v2")
        q_emb = model.encode([query]).astype("float32")
        faiss.normalize_L2(q_emb)
        scores, indices = index.search(q_emb, top_k)
        results = []
        for score, idx in zip(scores[0], indices[0]):
            if idx < 0 or idx >= len(meta):
                continue
            results.append({"score": float(score), **meta[idx]})
        return jsonify({"results": results})

    @app.route("/health")
    def health():
        return jsonify({"status": "ok", "index": str(INDEX_FILE)})

    print(f"RAG server on http://localhost:{port}")
    app.run(host="0.0.0.0", port=port)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="RAG index for Tarnish codebase")
    sub = parser.add_subparsers(dest="command")

    sub.add_parser("build", help="Build FAISS index from source files")
    
    qp = sub.add_parser("query", help="Query the index")
    qp.add_argument("query_text", help="Search query")
    qp.add_argument("--top-k", type=int, default=10)
    
    sp = sub.add_parser("serve", help="Start REST endpoint")
    sp.add_argument("--port", type=int, default=9191)

    args = parser.parse_args()
    
    if args.command == "build":
        build_index()
    elif args.command == "query":
        query_index(args.query_text, args.top_k)
    elif args.command == "serve":
        serve(args.port)
    else:
        parser.print_help()
