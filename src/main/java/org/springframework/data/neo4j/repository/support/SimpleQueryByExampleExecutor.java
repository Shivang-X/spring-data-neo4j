/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.repository.support;

import static org.neo4j.cypherdsl.core.Cypher.asterisk;

import java.util.List;
import java.util.Optional;
import java.util.function.LongSupplier;

import org.neo4j.cypherdsl.core.Functions;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.cypherdsl.core.StatementBuilder;
import org.neo4j.cypherdsl.core.StatementBuilder.BuildableStatement;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.core.Neo4jOperations;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.schema.CypherGenerator;
import org.springframework.data.neo4j.repository.query.CypherAdapterUtils;
import org.springframework.data.repository.query.QueryByExampleExecutor;
import org.springframework.data.repository.support.PageableExecutionUtils;

/**
 * A fragment for repositories providing "Query by example" functionality.
 *
 * @author Michael J. Simons
 * @author Ján Šúr
 * @param <T> type of the domain class
 * @since 1.0
 */
class SimpleQueryByExampleExecutor<T> implements QueryByExampleExecutor<T> {

	private final Neo4jOperations neo4jOperations;

	private final Neo4jMappingContext mappingContext;

	private final CypherGenerator cypherGenerator;

	SimpleQueryByExampleExecutor(Neo4jOperations neo4jOperations, Neo4jMappingContext mappingContext) {

		this.neo4jOperations = neo4jOperations;
		this.mappingContext = mappingContext;
		this.cypherGenerator = CypherGenerator.INSTANCE;
	}

	@Override
	public <S extends T> Optional<S> findOne(Example<S> example) {

		Predicate predicate = Predicate.create(mappingContext, example);
		Statement statement = predicate.useWithReadingFragment(cypherGenerator::prepareMatchOf).returning(asterisk())
				.build();

		return this.neo4jOperations.findOne(statement, predicate.getParameters(), example.getProbeType());
	}

	@Override
	public <S extends T> List<S> findAll(Example<S> example) {

		Predicate predicate = Predicate.create(mappingContext, example);
		Statement statement = predicate.useWithReadingFragment(cypherGenerator::prepareMatchOf).returning(asterisk())
				.build();

		return this.neo4jOperations.findAll(statement, predicate.getParameters(), example.getProbeType());
	}

	@Override
	public <S extends T> List<S> findAll(Example<S> example, Sort sort) {

		Predicate predicate = Predicate.create(mappingContext, example);
		Statement statement = predicate.useWithReadingFragment(cypherGenerator::prepareMatchOf).returning(asterisk())
				.orderBy(CypherAdapterUtils.toSortItems(predicate.getNeo4jPersistentEntity(), sort)).build();

		return this.neo4jOperations.findAll(statement, predicate.getParameters(), example.getProbeType());
	}

	@Override
	public <S extends T> long count(Example<S> example) {

		Predicate predicate = Predicate.create(mappingContext, example);
		Statement statement = predicate.useWithReadingFragment(cypherGenerator::prepareMatchOf)
				.returning(Functions.count(asterisk())).build();

		return this.neo4jOperations.count(statement, predicate.getParameters());
	}

	@Override
	public <S extends T> boolean exists(Example<S> example) {
		return findAll(example).iterator().hasNext();
	}

	@Override
	public <S extends T> Page<S> findAll(Example<S> example, Pageable pageable) {

		Predicate predicate = Predicate.create(mappingContext, example);
		StatementBuilder.OngoingReadingAndReturn returning = predicate
				.useWithReadingFragment(cypherGenerator::prepareMatchOf).returning(asterisk());

		BuildableStatement returningWithPaging = CypherAdapterUtils.addPagingParameter(predicate.getNeo4jPersistentEntity(),
				pageable, returning);

		Statement statement = returningWithPaging.build();

		List<S> page = this.neo4jOperations.findAll(statement, predicate.getParameters(), example.getProbeType());
		LongSupplier totalCountSupplier = () -> this.count(example);
		return PageableExecutionUtils.getPage(page, pageable, totalCountSupplier);
	}
}
